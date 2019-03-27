/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.app.plugin.core.memory;

import static org.junit.Assert.*;

import java.awt.Component;
import java.awt.Container;

import javax.swing.*;
import javax.swing.table.TableModel;

import org.junit.*;

import docking.action.DockingActionIf;
import docking.widgets.OptionDialog;
import ghidra.app.plugin.core.codebrowser.CodeBrowserPlugin;
import ghidra.app.plugin.core.gotoquery.GoToServicePlugin;
import ghidra.app.plugin.core.misc.RegisterField;
import ghidra.app.plugin.core.navigation.NavigationHistoryPlugin;
import ghidra.app.util.AddressInput;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.database.ProgramBuilder;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.Memory;
import ghidra.test.AbstractGhidraHeadedIntegrationTest;
import ghidra.test.TestEnv;
import ghidra.util.task.TaskMonitorAdapter;

public class MemoryMapProvider3Test extends AbstractGhidraHeadedIntegrationTest {

	private TestEnv env;
	private PluginTool tool;
	private MemoryMapPlugin plugin;
	private MemoryMapProvider provider;
	private Program program;
	private Memory memory;
	private JTable table;
	private TableModel model;

	public MemoryMapProvider3Test() {
		super();
	}

	private Program buildProgram(String programName) throws Exception {
		ProgramBuilder builder = new ProgramBuilder(programName, ProgramBuilder._TOY);
		builder.createMemory(".text", Long.toHexString(0x1001000), 0x6600);
		builder.createMemory(".data", Long.toHexString(0x1008000), 0x600);
		builder.createMemory(".rsrc", Long.toHexString(0x100A000), 0x5400);
		builder.createMemory(".bound_import_table", Long.toHexString(0xF0000248), 0xA8);
		builder.createMemory(".debug_data", Long.toHexString(0xF0001300), 0x1C);
		return builder.getProgram();
	}

	@Before
	public void setUp() throws Exception {

		program = buildProgram("notepad");

		env = new TestEnv();
		tool = env.showTool(program);
		tool.addPlugin(NavigationHistoryPlugin.class.getName());
		tool.addPlugin(CodeBrowserPlugin.class.getName());

		tool.addPlugin(MemoryMapPlugin.class.getName());
		tool.addPlugin(GoToServicePlugin.class.getName());
		plugin = env.getPlugin(MemoryMapPlugin.class);

		memory = program.getMemory();

		showProvider();
	}

	@After
	public void tearDown() throws Exception {
		env.release(program);
		env.dispose();
	}

	@Test
	public void testSplitBlockSetup() throws Exception {

		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Split Block");
		performAction(action, false);
		assertTrue(action.isEnabled());

		// find the dialog for the add
		SplitBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), SplitBlockDialog.class, 2000);
		assertNotNull(d);
		JTextField blockOneName =
			(JTextField) findComponentByName(d.getComponent(), "BlockOneName");
		JTextField blockOneStart =
			(JTextField) findComponentByName(d.getComponent(), "BlockOneStart");
		AddressInput blockOneEnd =
			(AddressInput) findComponentByName(d.getComponent(), "BlockOneEnd");
		RegisterField blockOneLength =
			(RegisterField) findComponentByName(d.getComponent(), "BlockOneLength");

		JTextField blockTwoName =
			(JTextField) findComponentByName(d.getComponent(), "BlockTwoName");
		AddressInput blockTwoStart =
			(AddressInput) findComponentByName(d.getComponent(), "BlockTwoStart");
		JTextField blockTwoEnd = (JTextField) findComponentByName(d.getComponent(), "BlockTwoEnd");
		RegisterField blockTwoLength =
			(RegisterField) findComponentByName(d.getComponent(), "BlockTwoLength");
		JButton okButton = findButton(d.getComponent(), "OK");

		assertNotNull(blockOneName);
		assertNotNull(blockOneStart);
		assertNotNull(blockOneEnd);
		assertNotNull(blockOneLength);

		assertNotNull(blockTwoName);
		assertNotNull(blockTwoStart);
		assertNotNull(blockTwoEnd);
		assertNotNull(blockTwoLength);
		assertNotNull(okButton);

		assertEquals(".text", blockOneName.getText());
		assertEquals("01001000", blockOneStart.getText());
		assertEquals(getAddr(0x10075ff), blockOneEnd.getAddress());
		assertEquals("0x6600", "0x" + Long.toHexString(blockOneLength.getValue().longValue()));
		assertEquals(".text.split", blockTwoName.getText());
		assertEquals(getAddr(0x01001000), blockTwoStart.getAddress());
		assertEquals("010075ff", blockTwoEnd.getText());
		assertEquals("-- No Value --", blockTwoLength.getText());

		assertTrue(!blockOneName.isEnabled());
		assertTrue(!blockOneStart.isEnabled());
		assertTrue(blockOneEnd.isEnabled());
		assertTrue(blockOneLength.isEnabled());

		assertTrue(blockTwoName.isEnabled());
		assertTrue(blockTwoStart.isEnabled());
		assertTrue(!blockTwoEnd.isEnabled());
		assertTrue(blockTwoLength.isEnabled());

		assertTrue(!okButton.isEnabled());

		final JButton cancelButton = findButton(d.getComponent(), "Cancel");

		SwingUtilities.invokeAndWait(
			() -> cancelButton.getActionListeners()[0].actionPerformed(null));
	}

	@Test
	public void testSplitBlockByLength() throws Exception {
		// split the first block by changing the length
		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Split Block");
		performAction(action, false);

		// find the dialog for the add
		SplitBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), SplitBlockDialog.class, 2000);
		JTextField blockOneName =
			(JTextField) findComponentByName(d.getComponent(), "BlockOneName");
		assertNotNull(blockOneName);
		JTextField blockOneStart =
			(JTextField) findComponentByName(d.getComponent(), "BlockOneStart");
		assertNotNull(blockOneStart);
		final AddressInput blockOneEnd =
			(AddressInput) findComponentByName(d.getComponent(), "BlockOneEnd");
		final RegisterField blockOneLength =
			(RegisterField) findComponentByName(d.getComponent(), "BlockOneLength");

		JTextField blockTwoName =
			(JTextField) findComponentByName(d.getComponent(), "BlockTwoName");
		assertNotNull(blockTwoName);
		final AddressInput blockTwoStart =
			(AddressInput) findComponentByName(d.getComponent(), "BlockTwoStart");
		final JTextField blockTwoEnd =
			(JTextField) findComponentByName(d.getComponent(), "BlockTwoEnd");
		final RegisterField blockTwoLength =
			(RegisterField) findComponentByName(d.getComponent(), "BlockTwoLength");
		final JButton okButton = findButton(d.getComponent(), "OK");

		SwingUtilities.invokeAndWait(() -> blockOneLength.setText("0x1000"));
		assertEquals(getAddr(0x01001fff), blockOneEnd.getAddress());
		assertEquals(getAddr(0x01002000), blockTwoStart.getAddress());
		assertEquals("010075ff", blockTwoEnd.getText());
		assertEquals(0x5600, blockTwoLength.getValue().longValue());
		assertTrue(okButton.isEnabled());

		SwingUtilities.invokeAndWait(() -> okButton.getActionListeners()[0].actionPerformed(null));

		program.flushEvents();
		waitForPostedSwingRunnables();

		assertEquals("01001000", model.getValueAt(0, MemoryMapModel.START));
		assertEquals("01001fff", model.getValueAt(0, MemoryMapModel.END));
		assertEquals("0x1000", model.getValueAt(0, MemoryMapModel.LENGTH));

		assertEquals(".text.split", model.getValueAt(1, MemoryMapModel.NAME));
		assertEquals("01002000", model.getValueAt(1, MemoryMapModel.START));
		assertEquals("010075ff", model.getValueAt(1, MemoryMapModel.END));
		assertEquals("0x5600", model.getValueAt(1, MemoryMapModel.LENGTH));
	}

	@Test
	public void testSplitBlockByEndAddress() throws Exception {
		// specify end address of block being split
		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Split Block");
		performAction(action, false);

		// find the dialog for the add
		SplitBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), SplitBlockDialog.class, 2000);
		final AddressInput blockOneEnd =
			(AddressInput) findComponentByName(d.getComponent(), "BlockOneEnd");
		final RegisterField blockOneLength =
			(RegisterField) findComponentByName(d.getComponent(), "BlockOneLength");

		final AddressInput blockTwoStart =
			(AddressInput) findComponentByName(d.getComponent(), "BlockTwoStart");
		final JTextField blockTwoEnd =
			(JTextField) findComponentByName(d.getComponent(), "BlockTwoEnd");
		final RegisterField blockTwoLength =
			(RegisterField) findComponentByName(d.getComponent(), "BlockTwoLength");
		final JButton okButton = findButton(d.getComponent(), "OK");

		SwingUtilities.invokeAndWait(() -> blockOneEnd.setValue("01003000"));
		assertEquals(0x2001, blockOneLength.getValue().longValue());
		assertEquals(getAddr(0x01003001), blockTwoStart.getAddress());
		assertEquals("010075ff", blockTwoEnd.getText());
		assertEquals(0x45ff, blockTwoLength.getValue().longValue());
		assertTrue(okButton.isEnabled());

		SwingUtilities.invokeAndWait(() -> okButton.getActionListeners()[0].actionPerformed(null));

		program.flushEvents();
		waitForPostedSwingRunnables();

		assertEquals("01001000", model.getValueAt(0, MemoryMapModel.START));
		assertEquals("01003000", model.getValueAt(0, MemoryMapModel.END));
		assertEquals("0x2001", model.getValueAt(0, MemoryMapModel.LENGTH));

		assertEquals(".text.split", model.getValueAt(1, MemoryMapModel.NAME));
		assertEquals("01003001", model.getValueAt(1, MemoryMapModel.START));
		assertEquals("010075ff", model.getValueAt(1, MemoryMapModel.END));
		assertEquals("0x45ff", model.getValueAt(1, MemoryMapModel.LENGTH));
	}

	@Test
	public void testSplitBlockNewStart() throws Exception {
		// specify start address of New block

		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Split Block");
		performAction(action, false);

		// find the dialog for the add
		SplitBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), SplitBlockDialog.class, 2000);

		final AddressInput blockOneEnd =
			(AddressInput) findComponentByName(d.getComponent(), "BlockOneEnd");
		final RegisterField blockOneLength =
			(RegisterField) findComponentByName(d.getComponent(), "BlockOneLength");

		final AddressInput blockTwoStart =
			(AddressInput) findComponentByName(d.getComponent(), "BlockTwoStart");
		final JTextField blockTwoEnd =
			(JTextField) findComponentByName(d.getComponent(), "BlockTwoEnd");
		final RegisterField blockTwoLength =
			(RegisterField) findComponentByName(d.getComponent(), "BlockTwoLength");
		final JButton okButton = findButton(d.getComponent(), "OK");

		SwingUtilities.invokeAndWait(() -> blockTwoStart.setValue("01003000"));
		assertEquals(0x2000, blockOneLength.getValue().longValue());
		assertEquals(getAddr(0x01002fff), blockOneEnd.getAddress());
		assertEquals("010075ff", blockTwoEnd.getText());
		assertEquals(0x4600, blockTwoLength.getValue().longValue());
		assertTrue(okButton.isEnabled());

		SwingUtilities.invokeAndWait(() -> okButton.getActionListeners()[0].actionPerformed(null));

		program.flushEvents();
		waitForPostedSwingRunnables();

		assertEquals("01001000", model.getValueAt(0, MemoryMapModel.START));
		assertEquals("01002fff", model.getValueAt(0, MemoryMapModel.END));
		assertEquals("0x2000", model.getValueAt(0, MemoryMapModel.LENGTH));

		assertEquals(".text.split", model.getValueAt(1, MemoryMapModel.NAME));
		assertEquals("01003000", model.getValueAt(1, MemoryMapModel.START));
		assertEquals("010075ff", model.getValueAt(1, MemoryMapModel.END));
		assertEquals("0x4600", model.getValueAt(1, MemoryMapModel.LENGTH));
	}

	@Test
	public void testSplitBlockNewLength() throws Exception {
		// specify the length of the New block
		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Split Block");
		performAction(action, false);

		// find the dialog for the add
		SplitBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), SplitBlockDialog.class, 2000);
		final AddressInput blockOneEnd =
			(AddressInput) findComponentByName(d.getComponent(), "BlockOneEnd");
		final RegisterField blockOneLength =
			(RegisterField) findComponentByName(d.getComponent(), "BlockOneLength");

		final JTextField blockTwoEnd =
			(JTextField) findComponentByName(d.getComponent(), "BlockTwoEnd");
		final RegisterField blockTwoLength =
			(RegisterField) findComponentByName(d.getComponent(), "BlockTwoLength");
		final JButton okButton = findButton(d.getComponent(), "OK");

		SwingUtilities.invokeAndWait(() -> blockTwoLength.setText("0x2000"));
		assertEquals(0x4600, blockOneLength.getValue().longValue());
		assertEquals(getAddr(0x010055ff), blockOneEnd.getAddress());
		assertEquals("010075ff", blockTwoEnd.getText());
		assertEquals(0x2000, blockTwoLength.getValue().longValue());
		assertTrue(okButton.isEnabled());

		SwingUtilities.invokeAndWait(() -> okButton.getActionListeners()[0].actionPerformed(null));

		program.flushEvents();
		waitForPostedSwingRunnables();

		assertEquals("01001000", model.getValueAt(0, MemoryMapModel.START));
		assertEquals("010055ff", model.getValueAt(0, MemoryMapModel.END));
		assertEquals("0x4600", model.getValueAt(0, MemoryMapModel.LENGTH));

		assertEquals(".text.split", model.getValueAt(1, MemoryMapModel.NAME));
		assertEquals("01005600", model.getValueAt(1, MemoryMapModel.START));
		assertEquals("010075ff", model.getValueAt(1, MemoryMapModel.END));
		assertEquals("0x2000", model.getValueAt(1, MemoryMapModel.LENGTH));

	}

	@Test
	public void testSplitBlockInvalidEnd() throws Exception {
		// enter address < than the start

		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Split Block");
		performAction(action, false);

		// find the dialog for the add
		final SplitBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), SplitBlockDialog.class, 2000);
		final AddressInput blockOneEnd =
			(AddressInput) findComponentByName(d.getComponent(), "BlockOneEnd");
		final JButton okButton = findButton(d.getComponent(), "OK");

		SwingUtilities.invokeAndWait(() -> blockOneEnd.setValue("01000"));
		assertTrue(!okButton.isEnabled());
		assertEquals("End address must be greater than start",
			findLabelStr(d.getComponent(), "statusLabel"));
		runSwing(() -> d.close());
	}

	@Test
	public void testSplitBlockInvalidNewStart() throws Exception {
		// enter address < original block start
		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Split Block");
		performAction(action, false);

		// find the dialog for the add
		final SplitBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), SplitBlockDialog.class, 2000);
		final AddressInput blockTwoStart =
			(AddressInput) findComponentByName(d.getComponent(), "BlockTwoStart");
		final JButton okButton = findButton(d.getComponent(), "OK");

		SwingUtilities.invokeAndWait(() -> blockTwoStart.setValue("01000"));
		assertTrue(!okButton.isEnabled());
		assertEquals("Start address must be greater than original block start (01001000)",
			findLabelStr(d.getComponent(), "statusLabel"));
		runSwing(() -> d.close());
	}

	@Test
	public void testSplitBlockInvalidLength() throws Exception {
		// clear block length; the ok button should be disabled
		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Split Block");
		performAction(action, false);

		// find the dialog for the add
		final SplitBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), SplitBlockDialog.class, 2000);
		final RegisterField blockOneLength =
			(RegisterField) findComponentByName(d.getComponent(), "BlockOneLength");
		final JButton okButton = findButton(d.getComponent(), "OK");

		SwingUtilities.invokeAndWait(() -> blockOneLength.setText(""));
		assertTrue(!okButton.isEnabled());
		runSwing(() -> d.close());

	}

	@Test
	public void testSplitBlockInvalidName() throws Exception {
		// enter illegal chars; the message should start with
		// "Invalid Block Name"
		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Split Block");
		performAction(action, false);

		// find the dialog for the add
		final SplitBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), SplitBlockDialog.class, 2000);

		final JTextField blockTwoName =
			(JTextField) findComponentByName(d.getComponent(), "BlockTwoName");
		final RegisterField blockTwoLength =
			(RegisterField) findComponentByName(d.getComponent(), "BlockTwoLength");
		final JButton okButton = findButton(d.getComponent(), "OK");

		SwingUtilities.invokeAndWait(() -> {
			blockTwoLength.setText("0x2000");
			blockTwoName.setText("split &%");
		});
		assertTrue(okButton.isEnabled());
		SwingUtilities.invokeAndWait(() -> okButton.getActionListeners()[0].actionPerformed(null));

		assertTrue(findLabelStr(d.getComponent(), "statusLabel").startsWith("Invalid Block Name"));
		runSwing(() -> d.close());
	}

	@Test
	public void testSplitNotAllowed() throws Exception {
		// create an overlay block
		int transactionID = program.startTransaction("test");
		memory.createInitializedBlock(".overlay", getAddr(0), 0x100, (byte) 0xa,
			TaskMonitorAdapter.DUMMY_MONITOR, true);
		program.endTransaction(transactionID, true);
		program.flushEvents();
		waitForPostedSwingRunnables();
		int row = table.getModel().getRowCount() - 1;
		table.setRowSelectionInterval(row, row);
		DockingActionIf action = getAction(plugin, "Split Block");
		performAction(action, false);
		final OptionDialog d =
			waitForDialogComponent(tool.getToolFrame(), OptionDialog.class, 2000);
		assertNotNull(d);
		assertEquals("Split Overlay Block Not Allowed", d.getTitle());
		runSwing(() -> d.close());
	}

	@Test
	public void testExpandBlockNotAllowed() throws Exception {
		// create an overlay block
		int transactionID = program.startTransaction("test");
		memory.createInitializedBlock(".overlay", getAddr(0), 0x100, (byte) 0xa,
			TaskMonitorAdapter.DUMMY_MONITOR, true);
		program.endTransaction(transactionID, true);
		program.flushEvents();
		waitForPostedSwingRunnables();
		int row = table.getModel().getRowCount() - 1;
		table.setRowSelectionInterval(row, row);
		DockingActionIf action = getAction(plugin, "Expand Block Up");
		performAction(action, false);
		final OptionDialog d =
			waitForDialogComponent(tool.getToolFrame(), OptionDialog.class, 2000);
		assertNotNull(d);
		assertEquals("Expand Overlay Block Not Allowed", d.getTitle());
		runSwing(() -> d.close());

		action = getAction(plugin, "Expand Block Down");
		performAction(action, false);

		final OptionDialog d2 =
			waitForDialogComponent(tool.getToolFrame(), OptionDialog.class, 2000);
		assertNotNull(d2);
		assertEquals("Expand Overlay Block Not Allowed", d2.getTitle());
		runSwing(() -> d2.close());

	}

	@Test
	public void testExpandBlockUpSetup() {

		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Expand Block Up");
		performAction(action, false);
		assertTrue(action.isEnabled());

		// find the dialog for the add
		final ExpandBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), ExpandBlockDialog.class, 2000);
		assertNotNull(d);
		assertEquals("Expand Block Up", d.getTitle());

		final AddressInput start =
			(AddressInput) findComponentByName(d.getComponent(), "NewStartAddress");
		final JTextField end = (JTextField) findComponentByName(d.getComponent(), "EndAddress");
		final RegisterField length =
			(RegisterField) findComponentByName(d.getComponent(), "BlockLength");
		final JButton okButton = findButton(d.getComponent(), "OK");

		assertNotNull(start);
		assertNotNull(end);
		assertNotNull(length);
		assertNotNull(okButton);

		assertTrue(!okButton.isEnabled());
		assertTrue(!end.isEnabled());
		assertEquals(getAddr(0x01001000), start.getAddress());
		assertEquals("010075ff", end.getText());
		assertEquals("0x6600", length.getText());
		runSwing(() -> d.close());
	}

	@Test
	public void testExpandBlockUpAddress() throws Exception {
		// expand block up by entering a new start address for the block
		// (must be less than the current start)
		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Expand Block Up");
		performAction(action, false);
		assertTrue(action.isEnabled());

		// find the dialog for the add
		ExpandBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), ExpandBlockDialog.class, 2000);

		final AddressInput start =
			(AddressInput) findComponentByName(d.getComponent(), "NewStartAddress");
		final JTextField end = (JTextField) findComponentByName(d.getComponent(), "EndAddress");
		assertNotNull(end);
		final RegisterField length =
			(RegisterField) findComponentByName(d.getComponent(), "BlockLength");
		final JButton okButton = findButton(d.getComponent(), "OK");

		SwingUtilities.invokeAndWait(() -> start.setValue("00002000"));
		assertEquals("0x1005600", length.getText());

		assertTrue(okButton.isEnabled());
		SwingUtilities.invokeAndWait(() -> okButton.getActionListeners()[0].actionPerformed(null));
		waitForPostedSwingRunnables();

		assertEquals(".text.exp", model.getValueAt(0, MemoryMapModel.NAME));
		assertEquals("00002000", model.getValueAt(0, MemoryMapModel.START));
		assertEquals("010075ff", model.getValueAt(0, MemoryMapModel.END));
		assertEquals("0x1005600", model.getValueAt(0, MemoryMapModel.LENGTH));

		undo(program);
		assertEquals(".text", model.getValueAt(0, MemoryMapModel.NAME));
		assertEquals("01001000", model.getValueAt(0, MemoryMapModel.START));
		assertEquals("010075ff", model.getValueAt(0, MemoryMapModel.END));
		assertEquals("0x6600", model.getValueAt(0, MemoryMapModel.LENGTH));

		redo(program);
		assertEquals(".text.exp", model.getValueAt(0, MemoryMapModel.NAME));
		assertEquals("00002000", model.getValueAt(0, MemoryMapModel.START));
		assertEquals("010075ff", model.getValueAt(0, MemoryMapModel.END));
		assertEquals("0x1005600", model.getValueAt(0, MemoryMapModel.LENGTH));

	}

	@Test
	public void testExpandBlockUpInvalidAddress() throws Exception {
		// enter start address that is greater than the current start
		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Expand Block Up");
		performAction(action, false);
		assertTrue(action.isEnabled());

		// find the dialog for the add
		final ExpandBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), ExpandBlockDialog.class, 2000);

		final AddressInput start =
			(AddressInput) findComponentByName(d.getComponent(), "NewStartAddress");
		final RegisterField length =
			(RegisterField) findComponentByName(d.getComponent(), "BlockLength");
		assertNotNull(length);
		final JButton okButton = findButton(d.getComponent(), "OK");

		SwingUtilities.invokeAndWait(() -> start.setValue("01201000"));
		assertTrue(!okButton.isEnabled());
		assertEquals("Start must be less than 01001000",
			findLabelStr(d.getComponent(), "statusLabel"));
		runSwing(() -> d.close());
	}

	@Test
	public void testExpandBlockUpLength() throws Exception {
		// expand block up by entering a new block length
		//(must be greater than current block length)
		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Expand Block Up");
		performAction(action, false);
		assertTrue(action.isEnabled());

		// find the dialog for the add
		ExpandBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), ExpandBlockDialog.class, 2000);
		final AddressInput start =
			(AddressInput) findComponentByName(d.getComponent(), "NewStartAddress");
		final JTextField end = (JTextField) findComponentByName(d.getComponent(), "EndAddress");
		final RegisterField length =
			(RegisterField) findComponentByName(d.getComponent(), "BlockLength");
		final JButton okButton = findButton(d.getComponent(), "OK");

		SwingUtilities.invokeAndWait(() -> length.setText("0x7600"));
		assertEquals(getAddr(0x01000000), start.getAddress());
		assertEquals("010075ff", end.getText());

		assertTrue(okButton.isEnabled());
		SwingUtilities.invokeAndWait(() -> okButton.getActionListeners()[0].actionPerformed(null));
		waitForPostedSwingRunnables();

		assertEquals(".text.exp", model.getValueAt(0, MemoryMapModel.NAME));
		assertEquals("01000000", model.getValueAt(0, MemoryMapModel.START));
		assertEquals("010075ff", model.getValueAt(0, MemoryMapModel.END));
		assertEquals("0x7600", model.getValueAt(0, MemoryMapModel.LENGTH));

		undo(program);
		assertEquals(".text", model.getValueAt(0, MemoryMapModel.NAME));
		assertEquals("01001000", model.getValueAt(0, MemoryMapModel.START));
		assertEquals("010075ff", model.getValueAt(0, MemoryMapModel.END));
		assertEquals("0x6600", model.getValueAt(0, MemoryMapModel.LENGTH));

		redo(program);
		assertEquals(".text.exp", model.getValueAt(0, MemoryMapModel.NAME));
		assertEquals("01000000", model.getValueAt(0, MemoryMapModel.START));
		assertEquals("010075ff", model.getValueAt(0, MemoryMapModel.END));
		assertEquals("0x7600", model.getValueAt(0, MemoryMapModel.LENGTH));
	}

	@Test
	public void testExpandBlockUpInvalidLength() throws Exception {
		// enter block length that is less than the current block length

		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Expand Block Up");
		performAction(action, false);
		assertTrue(action.isEnabled());

		// find the dialog for the add
		final ExpandBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), ExpandBlockDialog.class, 2000);

		final AddressInput start =
			(AddressInput) findComponentByName(d.getComponent(), "NewStartAddress");
		assertNotNull(start);
		final RegisterField length =
			(RegisterField) findComponentByName(d.getComponent(), "BlockLength");
		final JButton okButton = findButton(d.getComponent(), "OK");

		SwingUtilities.invokeAndWait(() -> length.setText("0x1000"));

		assertTrue(!okButton.isEnabled());
		assertEquals("Block size must be greater than 6600",
			findLabelStr(d.getComponent(), "statusLabel"));
		runSwing(() -> d.close());
	}

	@Test
	public void testExpandBlockUpOverlap() throws Exception {
		// attempt to expand a block that would result in an overlap
		// with another block

		table.setRowSelectionInterval(2, 2);

		DockingActionIf action = getAction(plugin, "Expand Block Up");
		performAction(action, false);
		assertTrue(action.isEnabled());

		// find the dialog for the add
		final ExpandBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), ExpandBlockDialog.class, 2000);

		final AddressInput start =
			(AddressInput) findComponentByName(d.getComponent(), "NewStartAddress");
		final JTextField end = (JTextField) findComponentByName(d.getComponent(), "EndAddress");
		final JButton okButton = findButton(d.getComponent(), "OK");

		SwingUtilities.invokeAndWait(() -> start.setValue("01008000"));
		assertEquals("0100f3ff", end.getText());
		assertTrue(okButton.isEnabled());

		SwingUtilities.invokeAndWait(() -> okButton.getActionListeners()[0].actionPerformed(null));
		waitForPostedSwingRunnables();
		assertTrue(!okButton.isEnabled());
		assertEquals("Part of range (01008000, 01009fff) already exists in memory.",
			findLabelStr(d.getComponent(), "statusLabel"));
		runSwing(() -> d.close());
	}

	@Test
	public void testExpandBlockDownSetup() {
		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Expand Block Down");
		performAction(action, false);
		assertTrue(action.isEnabled());

		// find the dialog for the add
		final ExpandBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), ExpandBlockDialog.class, 2000);
		assertNotNull(d);
		assertEquals("Expand Block Down", d.getTitle());

		final JTextField start = (JTextField) findComponentByName(d.getComponent(), "StartAddress");
		final AddressInput end = (AddressInput) findComponentByName(d.getComponent(), "EndAddress");
		final RegisterField length =
			(RegisterField) findComponentByName(d.getComponent(), "BlockLength");
		final JButton okButton = findButton(d.getComponent(), "OK");

		assertNotNull(start);
		assertNotNull(end);
		assertNotNull(length);
		assertNotNull(okButton);

		assertTrue(!okButton.isEnabled());
		assertTrue(!start.isEnabled());
		assertTrue(end.isEnabled());
		assertEquals("01001000", start.getText());
		assertEquals(getAddr(0x010075ff), end.getAddress());
		assertEquals("0x6600", length.getText());

		runSwing(() -> d.close());
	}

	@Test
	public void testExpandBlockDownEndAddress() throws Exception {
		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Expand Block Down");
		performAction(action, false);

		// find the dialog for the add
		ExpandBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), ExpandBlockDialog.class, 2000);

		final AddressInput end = (AddressInput) findComponentByName(d.getComponent(), "EndAddress");
		final RegisterField length =
			(RegisterField) findComponentByName(d.getComponent(), "BlockLength");
		final JButton okButton = findButton(d.getComponent(), "OK");

		SwingUtilities.invokeAndWait(() -> end.setValue("01007700"));
		assertEquals("0x6701", length.getText());
		assertTrue(okButton.isEnabled());

		SwingUtilities.invokeAndWait(() -> okButton.getActionListeners()[0].actionPerformed(null));
		waitForPostedSwingRunnables();

		assertEquals(".text.exp", model.getValueAt(0, MemoryMapModel.NAME));
		assertEquals("01001000", model.getValueAt(0, MemoryMapModel.START));
		assertEquals("01007700", model.getValueAt(0, MemoryMapModel.END));
		assertEquals("0x6701", model.getValueAt(0, MemoryMapModel.LENGTH));
	}

	@Test
	public void testExpandBlockDownLength() throws Exception {
		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Expand Block Down");
		performAction(action, false);

		// find the dialog for the add
		ExpandBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), ExpandBlockDialog.class, 2000);

		final AddressInput end = (AddressInput) findComponentByName(d.getComponent(), "EndAddress");
		final RegisterField length =
			(RegisterField) findComponentByName(d.getComponent(), "BlockLength");
		final JButton okButton = findButton(d.getComponent(), "OK");

		SwingUtilities.invokeAndWait(() -> length.setText("0x6700"));
		assertEquals(getAddr(0x10076ff), end.getAddress());
		assertTrue(okButton.isEnabled());

		SwingUtilities.invokeAndWait(() -> okButton.getActionListeners()[0].actionPerformed(null));
		waitForPostedSwingRunnables();

		assertEquals(".text.exp", model.getValueAt(0, MemoryMapModel.NAME));
		assertEquals("01001000", model.getValueAt(0, MemoryMapModel.START));
		assertEquals("010076ff", model.getValueAt(0, MemoryMapModel.END));
		assertEquals("0x6700", model.getValueAt(0, MemoryMapModel.LENGTH));

	}

	@Test
	public void testExpandBlockDownInvalidAddress() throws Exception {
		// enter an address that is less than the end address of the block
		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Expand Block Down");
		performAction(action, false);

		// find the dialog for the add
		final ExpandBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), ExpandBlockDialog.class, 2000);

		final AddressInput end = (AddressInput) findComponentByName(d.getComponent(), "EndAddress");
		final JButton okButton = findButton(d.getComponent(), "OK");

		SwingUtilities.invokeAndWait(() -> end.setValue("01007000"));
		assertTrue(!okButton.isEnabled());
		assertEquals("End must be greater than 010075ff",
			findLabelStr(d.getComponent(), "statusLabel"));
		runSwing(() -> d.close());
	}

	@Test
	public void testExpandBlockDownInvalidLength() throws Exception {
		// specify block length less than block size
		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Expand Block Down");
		performAction(action, false);

		// find the dialog for the add
		final ExpandBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), ExpandBlockDialog.class, 2000);

		final RegisterField length =
			(RegisterField) findComponentByName(d.getComponent(), "BlockLength");
		final JButton okButton = findButton(d.getComponent(), "OK");

		SwingUtilities.invokeAndWait(() -> length.setText("0x670"));
		assertTrue(!okButton.isEnabled());
		assertEquals("Block size must be greater than 6600",
			findLabelStr(d.getComponent(), "statusLabel"));
		runSwing(() -> d.close());
	}

	@Test
	public void testExpandBlockDownOverlap() throws Exception {

		table.setRowSelectionInterval(0, 0);

		DockingActionIf action = getAction(plugin, "Expand Block Down");
		performAction(action, false);

		// find the dialog for the add
		final ExpandBlockDialog d =
			waitForDialogComponent(tool.getToolFrame(), ExpandBlockDialog.class, 2000);

		final RegisterField length =
			(RegisterField) findComponentByName(d.getComponent(), "BlockLength");
		final JButton okButton = findButton(d.getComponent(), "OK");

		SwingUtilities.invokeAndWait(() -> length.setText("0x7600"));
		assertTrue(okButton.isEnabled());
		SwingUtilities.invokeAndWait(() -> okButton.getActionListeners()[0].actionPerformed(null));
		waitForPostedSwingRunnables();

		assertTrue(!okButton.isEnabled());
		assertEquals("Part of range (01007600, 010085ff) already exists in memory.",
			findLabelStr(d.getComponent(), "statusLabel"));
		runSwing(() -> d.close());
	}

	///////////////////////////////////////////////////////////////////

	private void showProvider() {
		DockingActionIf action = getAction(plugin, "View Memory Map");
		performAction(action, true);
		waitForPostedSwingRunnables();
		provider = plugin.getMemoryMapProvider();
		table = provider.getTable();
		model = table.getModel();
	}

	private Address getAddr(long offset) {
		return program.getMinAddress().getNewAddress(offset);
	}

	private JButton findButton(Container container, String text) {
		Component[] c = container.getComponents();
		for (Component element : c) {
			if (element instanceof JButton) {
				if (text.equals(((JButton) element).getText())) {
					return (JButton) element;
				}
			}
			if (element instanceof Container) {
				JButton b = findButton((Container) element, text);
				if (b != null) {
					return b;
				}
			}
		}
		return null;
	}

	private String findLabelStr(Container container, String name) {
		JLabel label = (JLabel) findComponentByName(container, name);
		if (label != null) {
			return label.getText();
		}
		return null;
	}

}