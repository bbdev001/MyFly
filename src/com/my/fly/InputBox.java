package com.my.fly;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class InputBox extends Dialog implements View.OnClickListener
{
	protected EditText input;
	protected TextView caption;
	
	public interface OnDialogClosedListener
	{
		public void OnClosed(boolean isCancel, String result);
	}
	protected OnDialogClosedListener onClosedListener = null;
	protected Activity context = null;

	public InputBox(Activity context, String title, String caption, String defaultValue, boolean isNumeric, OnDialogClosedListener onClosedListener)
	{
		super(context);
		this.context = context;
		setContentView(R.layout.input_box);
		setTitle(title);
	
		this.onClosedListener = onClosedListener;
		input = (EditText) findViewById(R.id.textLine);
		input.setText(defaultValue);
		
		if (isNumeric)
			input.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
				
		this.caption = (TextView) findViewById(R.id.captionLine);
		this.caption.setText(caption);
			
		((Button) findViewById(R.id.btnOk)).setOnClickListener(this);
		((Button) findViewById(R.id.btnCancel)).setOnClickListener(this);

		super.show();
	}

	@Override
	public void onClick(View v)
	{
		boolean result = false;
		
		if (v.getId() == R.id.btnCancel)
			result = true;

		onClosedListener.OnClosed(result, input.getText().toString());
		
		dismiss();
	}
}
