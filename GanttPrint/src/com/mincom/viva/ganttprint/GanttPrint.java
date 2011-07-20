package com.mincom.viva.ganttprint;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
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

	private final float[] dataWidths = new float[] { 60, 30, 250, 100, 100 };
	final float dataWidth;
	{
		int dw = 0;
		for (int i = 0; i < dataWidths.length; dw += dataWidths[i++])
			;
		dataWidth = dw;
	}
	private final float[] totalWidths = new float[dataWidths.length + 1];
	private final float barWidth;
	private final PdfPTable table = new PdfPTable(totalWidths.length);
	{
		table.setHorizontalAlignment(PdfPTable.ALIGN_LEFT);
		table.setLockedWidth(true);
	}
	private final PdfWriter pdfWriter;
	private final Document document;
	private final ByteArrayOutputStream baos;
	private final Schedule schedule;
	final SIZE size;

	private final SimpleDateFormat sdf = new SimpleDateFormat(
			"dd/MM/yyyy HH:mm");
	{
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	DateTime first = null, last = null;
	private long range;

	enum SCALE_LEVEL {
		DAILY, WEEKLY, MONTHLY;
	}

	SCALE_LEVEL scaleLevel;

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
			for (int i = 0; i < dataWidths.length; i++)
				totalWidths[i] = dataWidths[i];
			barWidth = size.rectangle.getWidth() - dataWidth - BORDER_PADDING
					* 2 - 2;
			totalWidths[totalWidths.length - 1] = barWidth;
			pdfWriter = PdfWriter.getInstance(document = new Document(
					this.size.rectangle),
					this.baos = new ByteArrayOutputStream());
			pdfWriter.setPageEvent(new PdfPageEventImpl(this));
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
				printSummary();
			}
			document.close();
			baos.flush();
			logger.debug("created document for gantt print");
		} catch (Exception e) {
			logger.debug("exception creating document for gantt print", e);
			throw new RuntimeException(e);
		}
	}

	private void establishDateRange() {
		for (ScheduleItem si : schedule) {
			DateTime start = new DateTime(si.getStart());
			if (first == null)
				first = new DateTime(start);
			if (start.isBefore(first))
				first = start;
			DateTime finish = new DateTime(si.getFinish());
			if (last == null)
				last = new DateTime(finish);
			if (finish.isAfter(last))
				last = finish;
		}
		logger.debug("first: {}", first);
		logger.debug("last: {}", last);
		if (first.isAfter(last))
			throw new IllegalStateException("first start date [" + first
					+ "] is after last finish date [" + last + "]");
		padDateRange();
		establishScaleLevel();
	}

	private void padDateRange() {
		DateTime f = first;
		DateTime l = last;

		/* pad out to week boundaries */
		f = f.withMillisOfDay(0).withDayOfWeek(1);
		l = l.withMillisOfDay(0).withDayOfWeek(1).plusWeeks(1).minusSeconds(1);

		/* if close to week boundaries pad out by another week */
		if (new Interval(f.getMillis(), first.getMillis()).toDuration()
				.isShorterThan(Duration.standardDays(2))) {
			f = f.minusWeeks(1);
		}
		if (new Interval(last.getMillis(), l.getMillis()).toDuration()
				.isShorterThan(Duration.standardDays(2))) {
			l = l.plusWeeks(1);
		}

		/* set state */
		first = f;
		last = l;
		range = last.getMillis() - first.getMillis();
	}

	private void establishScaleLevel() {
		Duration d = new Duration(range);
		if (d.isLongerThan(Duration.standardDays(365))) {
			scaleLevel = SCALE_LEVEL.MONTHLY;
		} else if (d.isLongerThan(Duration.standardDays(30))) {
			scaleLevel = SCALE_LEVEL.WEEKLY;
		} else {
			scaleLevel = SCALE_LEVEL.DAILY;
		}
	}

	private void printScheduleData() throws DocumentException {

		logger.debug("printing schedule ");

		table.setTotalWidth(totalWidths);

		/* set headers */
		table.addCell(newHeaderCell("Work Order"));
		table.addCell(newHeaderCell("Task"));
		table.addCell(newHeaderCell("Description"));
		table.addCell(newHeaderCell("Start"));
		table.addCell(newHeaderCell("Finish"));
		PdfPCell hc = newCell(/* empty */);
		hc.setCellEvent(new HeaderPdfPCellEventImpl(this));
		table.addCell(hc);
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
			PdfPCell c = newCell();
			c.setCellEvent(new PdfPCellEventImpl(this, si));
			table.addCell(c);
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

	float getX(long instant) {
		float x = (instant - first.getMillis()) * getScalingFactor();
		return x;
	}

	private float getScalingFactor() {
		return barWidth == 0 ? 1 : (barWidth / range);
	}

	private void printSummary() throws DocumentException {
		DateTimeFormatter dtf = DateTimeFormat.mediumDateTime();
		document.newPage();
		Paragraph p = new Paragraph("printed " + schedule.size()
				+ " schedule items, ranging between " + first.toString(dtf)
				+ " and " + last.toString(dtf));
		p.setAlignment(Paragraph.ALIGN_CENTER);
		p.setLeading(20);
		document.add(p);
		p = new Paragraph("printed at " + new DateTime().toString(dtf));
		p.setAlignment(Paragraph.ALIGN_CENTER);
		p.setLeading(20);
		document.add(p);
	}

	public byte[] getBytes() {
		return baos.toByteArray();
	}
}

