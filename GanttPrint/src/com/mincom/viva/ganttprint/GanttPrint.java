package com.mincom.viva.ganttprint;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEvent;
import com.lowagie.text.pdf.PdfWriter;

public class GanttPrint {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(GanttPrint.class);

	static final Font DATA_FONT = new Font(Font.HELVETICA, 9);
	static final Font HEADER_FONT = new Font(Font.HELVETICA, 8, Font.BOLD);
	static final int TABLE_WIDTH = 500;
	static final int ROW_HEIGHT = 19;
	static final int BORDER_PADDING = 5;

	public enum SIZE {
		A2(PageSize.A2.rotate()), A3(PageSize.A3.rotate()), A4(PageSize.A4
				.rotate());

		final Rectangle rectangle;

		SIZE(Rectangle r) {
			this.rectangle = r;
		}
	}

	private final PdfWriter pdfWriter;
	private final Document document;
	private final ByteArrayOutputStream baos;
	private final Schedule schedule;
	final SIZE size;

	private final SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy/MM/dd HH:mm");

	{
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	private PdfPTable table;
	private float dataWidth = 0;
	private float remainder = 0;
	Date first = null, last = null;

	public GanttPrint(Schedule s) {
		this(s, SIZE.A2);
	}

	public GanttPrint(Schedule schedule, SIZE size) {
		if (schedule == null)
			throw new IllegalArgumentException("require non-null model");
		this.schedule = schedule;
		if (size == null)
			throw new IllegalArgumentException("require non-null size");
		this.size = size;
		try {
			logger.debug("creating document for gantt print");
			pdfWriter = PdfWriter.getInstance(document = new Document(
					this.size.rectangle),
					this.baos = new ByteArrayOutputStream());
			pdfWriter.setPageEvent(new GanttPrintEventListener(this));
			document.addAuthor("Mincom Ltd");
			document.addTitle("Gantt Chart Print");
			document.setMargins(BORDER_PADDING + 1, BORDER_PADDING + 1,
					BORDER_PADDING + 1, BORDER_PADDING + 1);
			document.open();
			if (schedule.isEmpty()) {
				document.add(new Paragraph(BORDER_PADDING * 3,
						"   *** no data ***"));
			} else {
				establishDateRange();
				printScheduleData();
			}
			document.close();
			baos.flush();
			logger.debug("created document for gantt print");
		} catch (Exception e) {
			logger.debug("exception creating document for gantt print", e);
			throw new RuntimeException(e);
		}
	}

	private void establishDateRange() throws DocumentException {
		for (ScheduleItem si : schedule) {
			Date start = si.getStart();
			if (first == null)
				first = start;
			if (first.compareTo(start) > 0)
				first = start;
			Date finish = si.getFinish();
			if (last == null)
				last = finish;
			if (last.compareTo(finish) < 0)
				last = finish;
		}
		System.out.println("first: " + sdf.format(first));
		System.out.println("last: " + sdf.format(last));
	}

	private void printScheduleData() throws DocumentException {

		logger.debug("printing schedule ");

		float[] dataWidths = new float[] { 60, 30, 250, 100, 100 };
		for (float f : dataWidths)
			dataWidth += f;
		float[] totalWidths = new float[dataWidths.length + 1];
		for (int i = 0; i < dataWidths.length; i++)
			totalWidths[i] = dataWidths[i];
		remainder = size.rectangle.getWidth() - dataWidth - BORDER_PADDING * 2
				- 2;
		totalWidths[totalWidths.length - 1] = remainder;
		table = new PdfPTable(totalWidths.length);
		table.setHorizontalAlignment(PdfPTable.ALIGN_LEFT);
		table.setTotalWidth(totalWidths);
		table.setLockedWidth(true);

		/* set header rows */
		table.addCell(newHeaderCell("Work Order"));
		table.addCell(newHeaderCell("Task"));
		table.addCell(newHeaderCell("Description"));
		table.addCell(newHeaderCell("Start"));
		table.addCell(newHeaderCell("Finish"));
		table.addCell(newCell(/* empty */));
		table.setHeaderRows(1);

		logger.debug("schedule contains [{}] items", schedule.size());
		for (ScheduleItem si : schedule) {
			table.addCell(newDataCell(si.getWorkOrder()));
			table.addCell(newDataCell(si.getTaskNo()));
			table.addCell(newDataCell(si.getDescription()));
			String start = sdf.format(si.getStart());
			if (start == null)
				start = "null";
			table.addCell(newDataCell(start));
			String finish = sdf.format(si.getFinish());
			if (finish == null)
				finish = "null";
			table.addCell(newDataCell(finish));
			PdfPCell cell = newCell(/* empty */);
			cell.setCellEvent(new GanttPrintPdfPCellEvent(si));
			table.addCell(cell);
		}
		table.setComplete(true);
		document.add(table);
	}

	private PdfPCell newHeaderCell(String content) {
		PdfPCell cell = newCell();
		cell.addElement(newChunk(content, HEADER_FONT));
		return cell;
	}

	private PdfPCell newDataCell(String content) {
		PdfPCell cell = newCell();
		cell.addElement(newChunk(content, DATA_FONT));
		return cell;
	}

	private Chunk newChunk(String content, Font font) {
		Chunk chunk = new Chunk(content);
		chunk.setFont(font);
		return chunk;
	}

	private PdfPCell newCell() {
		PdfPCell cell = new PdfPCell();
		cell.setPaddingBottom(3);
		cell.setPaddingLeft(5);
		cell.setPaddingTop(0);
		cell.setPaddingRight(0);
		cell.setFixedHeight(ROW_HEIGHT);
		cell.setBorderColor(Color.gray);
		cell.setNoWrap(true);
		return cell;
	}

	public byte[] getBytes() {
		return baos.toByteArray();
	}
}

class GanttPrintEventListener implements PdfPageEvent {

	private final GanttPrint ganttPrint;

	public GanttPrintEventListener(GanttPrint ganttPrint) {
		this.ganttPrint = ganttPrint;
	}

	public void printBorders(PdfWriter writer) {
		PdfContentByte canvas = writer.getDirectContent();
		canvas.saveState();
		canvas.setLineWidth(1);
		canvas.setColorStroke(Color.black);
		Rectangle size = ganttPrint.size.rectangle;
		canvas.rectangle(GanttPrint.BORDER_PADDING, GanttPrint.BORDER_PADDING,
				size.getWidth() - GanttPrint.BORDER_PADDING * 2,
				size.getHeight() - GanttPrint.BORDER_PADDING * 2);
		canvas.stroke();
		canvas.restoreState();
	}

	@Override
	public void onOpenDocument(PdfWriter writer, Document document) {
	}

	@Override
	public void onStartPage(PdfWriter writer, Document document) {
		printBorders(writer);
	}

	@Override
	public void onEndPage(PdfWriter writer, Document document) {
	}

	@Override
	public void onCloseDocument(PdfWriter writer, Document document) {
	}

	@Override
	public void onParagraph(PdfWriter writer, Document document,
			float paragraphPosition) {
	}

	@Override
	public void onParagraphEnd(PdfWriter writer, Document document,
			float paragraphPosition) {
	}

	@Override
	public void onChapter(PdfWriter writer, Document document,
			float paragraphPosition, Paragraph title) {
	}

	@Override
	public void onChapterEnd(PdfWriter writer, Document document,
			float paragraphPosition) {
	}

	@Override
	public void onSection(PdfWriter writer, Document document,
			float paragraphPosition, int depth, Paragraph title) {
	}

	@Override
	public void onSectionEnd(PdfWriter writer, Document document,
			float paragraphPosition) {
	}

	@Override
	public void onGenericTag(PdfWriter writer, Document document,
			Rectangle rect, String text) {
	}

}

class GanttPrintPdfPCellEvent implements PdfPCellEvent {

	private final ScheduleItem scheduleItem;

	public GanttPrintPdfPCellEvent(ScheduleItem si) {
		this.scheduleItem = si;
	}

	@Override
	public void cellLayout(PdfPCell cell, Rectangle position,
			PdfContentByte[] canvases) {
		PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];
		canvas.saveState();
		canvas.setLineWidth(0.5f);
		canvas.setColorStroke(Color.blue);
		canvas.setColorFill(Color.decode("0xaaaaff"));
		float x = position.getLeft() + 2;
		float w = position.getWidth() - 40;
		float y = position.getBottom() + position.getHeight() / 3;
		float h = position.getHeight() / 3;
		canvas.rectangle(x, y, w, h);
		canvas.fillStroke();
		canvas.restoreState();
	}

}