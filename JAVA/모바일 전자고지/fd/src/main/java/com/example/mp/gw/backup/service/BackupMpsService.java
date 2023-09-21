package com.example.mp.gw.backup.service;


public interface BackupMpsService
{
	public void BackupPmMsg();
	public void Backup3DaysAgoPmMsg();
	public void BackupRetryPmMsgFromDate();

	public void BackupMpDcmntInfo();
	public void Backup3DaysAgoMpDcmntInfo();
	public void BackupRetryMpDcmntInfoFromDate();

	public void DeletePmRemindMsg();
	public void Delete3DaysAgoPmRemindMsg();
	public void DeleteRetryPmRemindMsgFromDate();
}
