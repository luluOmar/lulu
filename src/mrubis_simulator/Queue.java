package mrubis_simulator;

import java.util.LinkedList;

import org.eclipse.emf.common.notify.Notification;

public class Queue {
	
	/** List with all notifications of the last three runs */
	private LinkedList<LinkedList<Notification>> allNotifications = new LinkedList<LinkedList<Notification>>();
	/** Notifications of the actual feedback-loop */
	private LinkedList<Notification> notificationLevelA = new LinkedList<Notification>();
	/** Notifications of the last feedback-loop */
	private LinkedList<Notification> notificationLevelB = new LinkedList<Notification>();
	/** Notifications of the last but one feedback-loop */
	private LinkedList<Notification> notificationLevelC = new LinkedList<Notification>();

	public Queue() {
		allNotifications.add(notificationLevelA);
		allNotifications.add(notificationLevelB);
		allNotifications.add(notificationLevelC);
	}
	
	public void add(Notification notification) {
		this.allNotifications.get(0).add(notification);
	}
	
	public void remove(int historyLevel, Notification notification) {
		allNotifications.get(historyLevel).remove(notification);
	}
	
	public int getSize(int historyLevel) {
		return allNotifications.get(historyLevel).size();
	}
	
	public Notification get(int historyLevel, int index) {
		return allNotifications.get(historyLevel).get(index);
	}
	
	/**
	 * After each loop, this method must be called to push all notifications on the right history-level.
	 */
	public void initNewLoop() {
		notificationLevelC = notificationLevelB;
		notificationLevelB = notificationLevelA;
		notificationLevelA = new LinkedList<Notification>();
	}
}
