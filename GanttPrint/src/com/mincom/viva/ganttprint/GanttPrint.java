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
				printScheduleData();
				printScheduleBars();
			}
			document.close();
			baos.flush();
			logger.debug("created document for gantt print");
		} catch (Exception e) {
			logger.debug("exception creating document for gantt print", e);
			throw new RuntimeException(e);
		}
	}

	private void printScheduleData() throws DocumentException {

		logger.debug("printing schedule ");

		table = new PdfPTable(5);
		table.setHorizontalAlignment(PdfPTable.ALIGN_LEFT);
		table.setTotalWidth(TABLE_WIDTH);
		table.setLockedWidth(true);
		table.setWidths(new int[] { 9, 5, 35, 15, 15 });

		/* set header rows */
		table.addCell(newHeaderCell("Work Order"));
		table.addCell(newHeaderCell("Task"));
		table.addCell(newHeaderCell("Description"));
		table.addCell(newHeaderCell("Start"));
		table.addCell(newHeaderCell("Finish"));
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

	private void printScheduleBars() throws DocumentException {
		/* establish date range */
		Date first = null, last = null;
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
		PdfContentByte dcb = writer.getDirectContent();
		dcb.saveState();

		dcb.setLineWidth(1);
		dcb.setColorStroke(Color.black);
		Rectangle size = ganttPrint.size.rectangle;
		dcb.rectangle(GanttPrint.BORDER_PADDING, GanttPrint.BORDER_PADDING,
				size.getWidth() - GanttPrint.BORDER_PADDING * 2,
				size.getHeight() - GanttPrint.BORDER_PADDING * 2);
		dcb.stroke();

		dcb.restoreState();
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