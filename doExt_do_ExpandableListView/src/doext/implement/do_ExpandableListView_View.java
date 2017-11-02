package doext.implement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import core.DoServiceContainer;
import core.helper.DoJsonHelper;
import core.helper.DoScriptEngineHelper;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.interfaces.DoIListData;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoInvokeResult;
import core.object.DoMultitonModule;
import core.object.DoSourceFile;
import core.object.DoUIContainer;
import core.object.DoUIModule;
import doext.define.do_ExpandableListView_IMethod;
import doext.define.do_ExpandableListView_MAbstract;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,
 * do_ExpandableListView_IMethod接口； #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
public class do_ExpandableListView_View extends ExpandableListView implements DoIUIModuleView, do_ExpandableListView_IMethod, OnScrollListener {

	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_ExpandableListView_MAbstract model;
	protected MyAdapter myAdapter;
	private boolean allExpanded;
	private int oldFirstVisiblePosition;
	private int oldLastVisiblePosition;
	//是否有滚动效果,值为false不出发scroll事件
	protected boolean mIsSmooth = true;

	public do_ExpandableListView_View(Context context) {
		super(context);
		myAdapter = new MyAdapter();
	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(final DoUIModule _doUIModule) throws Exception {
		this.model = (do_ExpandableListView_MAbstract) _doUIModule;
		this.setGroupIndicator(null);
		this.setDivider(new ColorDrawable(Color.TRANSPARENT));
		this.setDividerHeight(0);
		this.setOnChildClickListener(new OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				DoInvokeResult _jsonResult = new DoInvokeResult(_doUIModule.getUniqueKey());
				JSONObject _val = new JSONObject();
				try {
					_val.put("groupIndex", groupPosition);
					_val.put("childIndex", childPosition);
					_jsonResult.setResultNode(_val);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				_doUIModule.getEventCenter().fireEvent("childTouch", _jsonResult);
				return false;
			}
		});

		this.setOnGroupClickListener(new OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				DoInvokeResult _jsonResult = new DoInvokeResult(_doUIModule.getUniqueKey());
				_jsonResult.setResultInteger(groupPosition);
				_doUIModule.getEventCenter().fireEvent("groupTouch", _jsonResult);
				return false;
			}
		});

		this.setOnGroupCollapseListener(new OnGroupCollapseListener() {
			@Override
			public void onGroupCollapse(int groupPosition) {
				DoInvokeResult _jsonResult = new DoInvokeResult(_doUIModule.getUniqueKey());
				_jsonResult.setResultInteger(groupPosition);
				_doUIModule.getEventCenter().fireEvent("groupCollapse", _jsonResult);
			}
		});

		this.setOnGroupExpandListener(new OnGroupExpandListener() {
			@Override
			public void onGroupExpand(int groupPosition) {
				DoInvokeResult _jsonResult = new DoInvokeResult(_doUIModule.getUniqueKey());
				_jsonResult.setResultInteger(groupPosition);
				_doUIModule.getEventCenter().fireEvent("groupExpand", _jsonResult);
			}
		});
		this.setOnScrollListener(this);
	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		return true;
	}

	/**
	 * 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public void onPropertiesChanged(Map<String, String> _changedValues) {
		DoUIModuleHelper.handleBasicViewProperChanged(this.model, _changedValues);

		if (_changedValues.containsKey("isShowbar")) {
			boolean _isShowbar = DoTextHelper.strToBool(_changedValues.get("isShowbar"), true);
			this.setVerticalScrollBarEnabled(_isShowbar);
		}
		if (_changedValues.containsKey("selectedColor")) {
			try {
				String _bgColor = this.model.getPropertyValue("bgColor");
				String _selectedColor = _changedValues.get("selectedColor");
				Drawable normal = new ColorDrawable(DoUIModuleHelper.getColorFromString(_bgColor, Color.WHITE));
				Drawable selected = new ColorDrawable(DoUIModuleHelper.getColorFromString(_selectedColor, Color.WHITE));
				Drawable pressed = new ColorDrawable(DoUIModuleHelper.getColorFromString(_selectedColor, Color.WHITE));
				this.setSelector(getBg(normal, selected, pressed));
			} catch (Exception _err) {
				DoServiceContainer.getLogEngine().writeError("do_ListView selectedColor \n\t", _err);
			}
		}

		if (_changedValues.containsKey("groupTemplate")) {
			initGroupTemplate(_changedValues.get("groupTemplate"));
		}

		if (_changedValues.containsKey("childTemplate")) {
			initChildTemplate(_changedValues.get("childTemplate"));
		}

		if (_changedValues.containsKey("allExpanded")) {
			allExpanded = DoTextHelper.strToBool(_changedValues.get("allExpanded"), false);
		}
	}

	private void initGroupTemplate(String data) {
		try {
			myAdapter.initGroupTemplates(data.split(","));
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("解析cell属性错误： \t", e);
		}
	}

	private void initChildTemplate(String data) {
		try {
			myAdapter.initChildTemplates(data.split(","));
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("解析cell属性错误： \t", e);
		}
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("bindItems".equals(_methodName)) {
			bindItems(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("collapseGroup".equals(_methodName)) {
			collapseGroup(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("expandGroup".equals(_methodName)) {
			expandGroup(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("refreshItems".equals(_methodName)) {
			refreshItems(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("scrollToPosition".equals(_methodName)) {
			scrollToPosition(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("refreshSpecifiedItems".equals(_methodName)) {
			refreshSpecifiedItems(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		return false;
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.model.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		return false;
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {

	}

	private void scrollToPosition(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws JSONException {
		int _groupIndex = DoJsonHelper.getInt(_dictParas, "groupIndex", 0);
		int _childIndex = DoJsonHelper.getInt(_dictParas, "childIndex", 0);
		mIsSmooth = DoJsonHelper.getBoolean(_dictParas, "isSmooth", false);
		expandGroup(_groupIndex);
		this.setSelectedChild(_groupIndex, _childIndex, false);
	}

	/**
	 * 重绘组件，构造组件时由系统框架自动调用；
	 * 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
	 */
	@Override
	public void onRedraw() {
		this.setLayoutParams(DoUIModuleHelper.getLayoutParams(this.model));
	}

	/**
	 * 获取当前model实例
	 */
	@Override
	public DoUIModule getModel() {
		return model;
	}

	/**
	 * 绑定item的数据；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void bindItems(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		String _groupDataAddress = DoJsonHelper.getString(_dictParas, "groupData", "");
		String _childDataAddress = DoJsonHelper.getString(_dictParas, "childData", "");
		DoMultitonModule _groupData = null;
		DoMultitonModule _childData = null;
		if (_groupDataAddress != null && _groupDataAddress.length() > 0) {
			_groupData = DoScriptEngineHelper.parseMultitonModule(_scriptEngine, _groupDataAddress);
		}

		if (_childDataAddress != null && _childDataAddress.length() > 0) {
			_childData = DoScriptEngineHelper.parseMultitonModule(_scriptEngine, _childDataAddress);
		}

		if (null != _groupData && _groupData instanceof DoIListData && null != _childData && _childData instanceof DoIListData) {
			myAdapter.bindData((DoIListData) _groupData, (DoIListData) _childData);
			this.setAdapter(myAdapter);
			myAdapter.expandAll();
		}
	}

	/**
	 * 收缩组；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void collapseGroup(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		JSONArray _array = DoJsonHelper.getJSONArray(_dictParas, "indexs");
		if (null != _array && _array.length() > 0) {
			for (int i = 0; i < _array.length(); i++) {
				int _index = _array.getInt(i);
				this.collapseGroup(_index);
			}
		}
	}

	/**
	 * 展开组；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void expandGroup(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		JSONArray _array = DoJsonHelper.getJSONArray(_dictParas, "indexs");
		if (null != _array && _array.length() > 0) {
			for (int i = 0; i < _array.length(); i++) {
				int _index = _array.getInt(i);
				this.expandGroup(_index, true);
			}
		}
	}

	/**
	 * 刷新item数据；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void refreshItems(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		myAdapter.notifyDataSetChanged();
	}

	private class MyAdapter extends BaseExpandableListAdapter {

		private DoIListData groupData;
		private DoIListData childData;

		private List<String> groupTemplates = new ArrayList<String>();
		private Map<String, String> viewGroupTemplates = new HashMap<String, String>();

		private List<String> childTemplates = new ArrayList<String>();
		private Map<String, String> viewChildTemplates = new HashMap<String, String>();

		private SparseIntArray groupPositionMap = new SparseIntArray();
		@SuppressLint("UseSparseArrays")
		private Map<Integer, SparseIntArray> childPositionMap = new HashMap<Integer, SparseIntArray>();

		public void initGroupTemplates(String[] templates) throws Exception {
			initTemplates(templates, groupTemplates, viewGroupTemplates);
		}

		public void initChildTemplates(String[] templates) throws Exception {
			initTemplates(templates, childTemplates, viewChildTemplates);
		}

		private void initTemplates(String[] templates, List<String> cellTemplates, Map<String, String> viewTemplates) throws Exception {
			for (String templateUi : templates) {
				if (templateUi != null && !templateUi.equals("")) {
					DoSourceFile _sourceFile = model.getCurrentPage().getCurrentApp().getSourceFS().getSourceByFileName(templateUi);
					if (_sourceFile != null) {
						viewTemplates.put(templateUi, _sourceFile.getTxtContent());
						cellTemplates.add(templateUi);
					} else {
						throw new Exception("试图使用一个无效的页面文件:" + templateUi);
					}
				}
			}
		}

		public void bindData(DoIListData _groupData, DoIListData _childData) {
			this.groupData = _groupData;
			this.childData = _childData;
			notifyDataSetChanged();
		}

		public void expandAll() {
			if (allExpanded) {
				int groupCount = getCount();
				for (int i = 0; i < groupCount; i++) {
					expandGroup(i);
				}
			}
		}

		@Override
		public void notifyDataSetChanged() {
			try {
				int _groupSize = groupData.getCount();
				for (int i = 0; i < _groupSize; i++) {
					JSONObject _childData = (JSONObject) groupData.getData(i);
					Integer _index = DoTextHelper.strToInt(DoJsonHelper.getString(_childData, "template", "0"), 0);
					if (_index >= groupTemplates.size() || _index < 0) {
						DoServiceContainer.getLogEngine().writeError("索引不存在", new Exception("索引 " + _index + " 不存在"));
						_index = 0;
					}
					groupPositionMap.put(i, _index);

					SparseIntArray _childPositionMap = new SparseIntArray();
					JSONArray _array = (JSONArray) childData.getData(i);
					int _childSize = _array.length();
					for (int j = 0; j < _childSize; j++) {
						_childData = _array.getJSONObject(j);
						_index = DoTextHelper.strToInt(DoJsonHelper.getString(_childData, "template", "0"), 0);
						if (_index >= childTemplates.size() || _index < 0) {
							DoServiceContainer.getLogEngine().writeError("索引不存在", new Exception("索引 " + _index + " 不存在"));
							_index = 0;
						}
						_childPositionMap.put(j, _index);
					}
					childPositionMap.put(i, _childPositionMap);
				}
			} catch (Exception e) {
				DoServiceContainer.getLogEngine().writeError("解析data数据错误： \t", e);
			}

			super.notifyDataSetChanged();
		}

		@Override
		public int getChildType(int groupPosition, int childPosition) {
			return childPositionMap.get(groupPosition).get(childPosition);
		}

		@Override
		public int getChildTypeCount() {
			if (childTemplates.size() == 0) {
				return super.getChildTypeCount();
			}
			return childTemplates.size();
		}

		@Override
		public int getGroupType(int groupPosition) {
			return groupPositionMap.get(groupPosition);
		}

		@Override
		public int getGroupTypeCount() {
			if (groupTemplates.size() == 0) {
				return super.getGroupTypeCount();
			}
			return groupTemplates.size();
		}

		@Override
		public int getGroupCount() {
			return groupData.getCount();
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			try {
				return ((JSONArray) childData.getData(groupPosition)).length();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return 0;
		}

		@Override
		public Object getGroup(int groupPosition) {
			try {
				return groupData.getData(groupPosition);
			} catch (JSONException e) {
				DoServiceContainer.getLogEngine().writeError("do_ExpandableListView_View getGroup \n\t", e);
			}
			return groupPosition;
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			try {
				return childData.getData(groupPosition);
			} catch (JSONException e) {
				DoServiceContainer.getLogEngine().writeError("do_ExpandableListView_View getChild \n\t", e);
			}
			return childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			View _childView = null;
			try {
				JSONObject _childData = (JSONObject) ((JSONArray) childData.getData(groupPosition)).getJSONObject(childPosition);
				DoIUIModuleView _doIUIModuleView = null;
				if (convertView == null) {
					int _index = DoTextHelper.strToInt(DoJsonHelper.getString(_childData, "template", "0"), 0);
					if (_index >= childTemplates.size() || _index < 0) {
						DoServiceContainer.getLogEngine().writeError("索引不存在", new Exception("索引 " + _index + " 不存在"));
						_index = 0;
					}
					String _templateUI = childTemplates.get(_index);
					String _content = viewChildTemplates.get(_templateUI);
					DoUIContainer _doUIContainer = new DoUIContainer(model.getCurrentPage());
					_doUIContainer.loadFromContent(_content, null, null);
					_doUIContainer.loadDefalutScriptFile(_templateUI);// @zhuozy效率问题，listview第一屏可能要加载多次模版、脚本，需改进需求设计；
					_doIUIModuleView = _doUIContainer.getRootView().getCurrentUIModuleView();
				} else {
					_doIUIModuleView = (DoIUIModuleView) convertView;
				}
				if (_doIUIModuleView != null) {
					_doIUIModuleView.getModel().setModelData(_childData);

					_childView = (View) _doIUIModuleView;
					// 设置headerView 的 宽高
					_childView.setLayoutParams(new AbsListView.LayoutParams((int) _doIUIModuleView.getModel().getRealWidth(), (int) _doIUIModuleView.getModel().getRealHeight()));
					if (_childView instanceof ViewGroup) {
						((ViewGroup) _childView).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
					}
				}
			} catch (Exception e) {
				DoServiceContainer.getLogEngine().writeError("解析data数据错误： \t", e);
			}
			if (_childView == null) {
				return new View(getContext());
			}
			return _childView;
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View _childView = null;
			try {
				JSONObject _childData = (JSONObject) groupData.getData(groupPosition);
				DoIUIModuleView _doIUIModuleView = null;
				if (convertView == null) {
					int _index = DoTextHelper.strToInt(DoJsonHelper.getString(_childData, "template", "0"), 0);
					if (_index >= groupTemplates.size() || _index < 0) {
						DoServiceContainer.getLogEngine().writeError("索引不存在", new Exception("索引 " + _index + " 不存在"));
						_index = 0;
					}
					String _templateUI = groupTemplates.get(_index);
					String _content = viewGroupTemplates.get(_templateUI);
					DoUIContainer _doUIContainer = new DoUIContainer(model.getCurrentPage());
					_doUIContainer.loadFromContent(_content, null, null);
					_doUIContainer.loadDefalutScriptFile(_templateUI);// @zhuozy效率问题，listview第一屏可能要加载多次模版、脚本，需改进需求设计；
					_doIUIModuleView = _doUIContainer.getRootView().getCurrentUIModuleView();
				} else {
					_doIUIModuleView = (DoIUIModuleView) convertView;
				}
				if (_doIUIModuleView != null) {
					_doIUIModuleView.getModel().setModelData(_childData);

					_childView = (View) _doIUIModuleView;
					// 设置headerView 的 宽高
					_childView.setLayoutParams(new AbsListView.LayoutParams((int) _doIUIModuleView.getModel().getRealWidth(), (int) _doIUIModuleView.getModel().getRealHeight()));
					if (_childView instanceof ViewGroup) {
						((ViewGroup) _childView).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
					}
				}
			} catch (Exception e) {
				DoServiceContainer.getLogEngine().writeError("解析data数据错误： \t", e);
			}
			if (_childView == null) {
				return new View(getContext());
			}
			return _childView;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {

	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (!mIsSmooth) {//false不触发scroll事件
			return;
		}
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		int firstVisiblePosition = view.getFirstVisiblePosition();
		int lastVisiblePosition = view.getLastVisiblePosition();
		if (lastVisiblePosition != -1) {
			if (oldFirstVisiblePosition == firstVisiblePosition && oldLastVisiblePosition == lastVisiblePosition) {
				return;
			}
			oldFirstVisiblePosition = firstVisiblePosition;
			oldLastVisiblePosition = lastVisiblePosition;
			try {
				JSONObject _node = new JSONObject();
				_node.put("firstVisiblePosition", firstVisiblePosition);
				_node.put("lastVisiblePosition", lastVisiblePosition);
				_invokeResult.setResultNode(_node);
				this.model.getEventCenter().fireEvent("scroll", _invokeResult);
			} catch (Exception _err) {
				DoServiceContainer.getLogEngine().writeError("do_ExpandableListView_View scroll" + " \n", _err);
			}
		}
	}

	private StateListDrawable getBg(Drawable normal, Drawable selected, Drawable pressed) {
		StateListDrawable bg = new StateListDrawable();
		bg.addState(View.PRESSED_ENABLED_STATE_SET, pressed);
		bg.addState(View.ENABLED_FOCUSED_STATE_SET, selected);
		bg.addState(View.ENABLED_STATE_SET, normal);
		bg.addState(View.FOCUSED_STATE_SET, selected);
		bg.addState(View.EMPTY_STATE_SET, normal);
		return bg;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent e) {
		switch (e.getAction()) {
		case MotionEvent.ACTION_MOVE:
			mIsSmooth = true;
			break;
		}
		return super.onInterceptTouchEvent(e);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_MOVE:
			mIsSmooth = true;
			break;
		}
		return super.onTouchEvent(event);
	}

	@Override
	public void refreshSpecifiedItems(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		myAdapter.notifyDataSetChanged();
	}

}