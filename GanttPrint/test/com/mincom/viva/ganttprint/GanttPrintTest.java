package com.mincom.viva.ganttprint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit38.AbstractJUnit38SpringContextTests;

@ContextConfiguration
public class GanttPrintTest extends AbstractJUnit38SpringContextTests {

	@Autowired
	protected SimpleJdbcTemplate simpleJdbcTemplate;

	private void assertions(byte[] bs) throws IOException {
		assertNotNull(bs);
		String fileName = System.getProperty("java.io.tmpdir") + File.separator
				+ getClass().getSimpleName() + "-" + System.currentTimeMillis()
				+ "-" + getName() + ".pdf";
		System.out.println("file name: " + fileName);
		FileOutputStream fos = new FileOutputStream(fileName);
		fos.write(bs);
		fos.flush();
		fos.close();
	}

	public void testEmpty() throws IOException {
		Schedule s = new Schedule();
		GanttPrint gp = new GanttPrint(s, GanttPrint.SIZE.A3);
		byte[] bs = gp.getBytes();
		assertions(bs);
	}

	public void testOne() throws IOException {
		Schedule s = new Schedule();
		s.add(newScheduleItem(0));
		GanttPrint gp = new GanttPrint(s, GanttPrint.SIZE.A3);
		byte[] bs = gp.getBytes();
		assertions(bs);
	}

	private ScheduleItem newScheduleItem(int i) {
		ScheduleItem si = new ScheduleItem();
		si.setWorkOrder("12345678");
		si.setTaskNo("001");
		si.setDescription("work order description has 40 characters");
		DateTime start = new DateTime().plusDays(i);
		si.setStart(start.toDate());
		DateTime finish = new DateTime(start).plusDays(4);
		si.setFinish(finish.toDate());
		return si;
	}

	public void testSome() throws IOException {
		Schedule s = new Schedule();
		for (int i = 0; i < 100; i++)
			s.add(newScheduleItem(i));
		GanttPrint gp = new GanttPrint(s, GanttPrint.SIZE.A3);
		byte[] bs = gp.getBytes();
		assertions(bs);
	}

	public void testMany() throws IOException {
		Schedule s = new Schedule();
		for (int i = 0; i < 1000; i++)
			s.add(newScheduleItem(i));
		GanttPrint gp = new GanttPrint(s, GanttPrint.SIZE.A3);
		byte[] bs = gp.getBytes();
		assertions(bs);
	}

//	public void testMany() throws IOException {
//		Schedule s = buildScheduleFromDatabase();
//		GanttPrint gp = new GanttPrint(s);
//		byte[] bs = gp.getBytes();
//		assertions(bs);
//	}

	private Schedule buildScheduleFromDatabase() {
		Schedule s = new Schedule();

		@SuppressWarnings({ "deprecation" })
		List<ScheduleItem> scheduleItems = simpleJdbcTemplate
				.query("SELECT * FROM MSF623 WHERE PLAN_STR_DATE != ' ' AND PLAN_FIN_DATE != ' ' AND ROWNUM < 5001",
						new ParameterizedRowMapper<ScheduleItem>() {

							@Override
							public ScheduleItem mapRow(ResultSet rs, int rowNum)
									throws SQLException {
								ScheduleItem si = new ScheduleItem();
								si.setWorkOrder(rs.getString("WORK_ORDER"));
								si.setTaskNo(rs.getString("WO_TASK_NO"));
								si.setDescription(rs.getString("WO_TASK_DESC"));
								Date startDate = parseDate(rs
										.getString("PLAN_STR_DATE"));
								Date start = parseDateTime(startDate,
										rs.getString("PLAN_STR_TIME"));
								si.setStart(start);
								Date finishDate = parseDate(rs
										.getString("PLAN_FIN_DATE"));
								Date finish = parseDateTime(finishDate,
										rs.getString("PLAN_FIN_TIME"));
								si.setFinish(finish);

								return si;
							}

							SimpleDateFormat df, tf;

							{
								df = new SimpleDateFormat("yyyyMMdd");
								df.setTimeZone(TimeZone.getTimeZone("GMT"));
								tf = new SimpleDateFormat("HHmmss");
								tf.setTimeZone(TimeZone.getTimeZone("GMT"));
							}

							private Date parseDate(String dateString) {
								try {
									return df.parse(dateString);
								} catch (ParseException e) {
									throw new RuntimeException(e);
								}
							}

							private Date parseDateTime(Date date,
									String timeString) {
								Date time = new Date(0);
								try {
									time = tf.parse(timeString);
								} catch (ParseException e) {
									// squash
								}
								return new Date(date.getTime() + time.getTime());
							}
						}, new Object[] {});

		s.addAll(scheduleItems);
		System.out.println(s.size() + " schedule items");
		return s;
	}

}
