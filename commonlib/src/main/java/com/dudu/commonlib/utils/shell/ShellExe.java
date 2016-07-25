/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.dudu.commonlib.utils.shell;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ShellExe
{
	private static Logger log = LoggerFactory.getLogger("ShellExe");

	public static String execCommand(String[] command) throws IOException
	{
		Runtime runtime = Runtime.getRuntime();
		Process proc = runtime.exec(command);

		Thread t = new Thread(new InputStreamRunnable(proc.getErrorStream(),"ErrorStream"));
		t.start();

		InputStream inputstream = proc.getInputStream();
		InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
		BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
		String line = "";
		StringBuilder sb = new StringBuilder(line);
		while ((line = bufferedreader.readLine()) != null)
		{
			sb.append(line);
			// sb.append('\n');
		}
		try
		{
			if (proc.waitFor() == 0)
				line = sb.toString();

		} catch (InterruptedException e)
		{
			System.err.println(e);
		} finally {

		}
		bufferedreader.close();
		inputstreamreader.close();
		LoggerFactory.getLogger("ShellExe").debug("执行shell命令结果：{}", line);
		return line;
	}

	static class InputStreamRunnable implements Runnable {
        BufferedReader bReader=null;
        String type=null;
        public InputStreamRunnable(InputStream is, String _type) {
            try {
                bReader=new BufferedReader(new InputStreamReader(new BufferedInputStream(is),"UTF-8"));
                type=_type;
            }
            catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        public void run() {
            String line;

            try {
                while((line=bReader.readLine())!=null) {
                    // do nothing
                }
            }
            catch(Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    bReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
		}
    }


	/**
	 * 执行shell命令时，不等待，不获取返回值
	 * @param command
	 * @return
	 * @throws IOException
     */
	public static String execCommandNoWait(String[] command) throws IOException
	{
		Runtime runtime = Runtime.getRuntime();
		Process proc = runtime.exec(command);

		String line = "";
		LoggerFactory.getLogger("ShellExe").debug("执行shell命令结果：{}", line);
		return line;
	}

	public synchronized static String execShellCmd(String command)
	{
		LoggerFactory.getLogger("ShellExe").debug("执行shell命令：{}", command);
		String[] arrayOfString = new String[3];
		arrayOfString[0] = "/system/bin/sh";
		arrayOfString[1] = "-c";
		arrayOfString[2] = command;
		try
		{
			return ShellExe.execCommand(arrayOfString);
		} catch (IOException e) {
			log.error("ShellExe 异常", e);
		}
		return "";
	}

	/**
	 * 执行shell命令时，不等待，不获取返回值
	 * @param command
	 * @return
     */
	public synchronized static String execShellCmdNoWait(String command)
	{
		LoggerFactory.getLogger("ShellExe").debug("执行shell命令：{}", command);
		String[] arrayOfString = new String[3];
		arrayOfString[0] = "/system/bin/sh";
		arrayOfString[1] = "-c";
		arrayOfString[2] = command;
		try
		{
			return ShellExe.execCommandNoWait(arrayOfString);
		} catch (IOException e) {
			log.error("ShellExe 异常", e);
		}
		return "";
	}
}
