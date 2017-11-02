package doext.define;

import core.object.DoUIModule;
import core.object.DoProperty;
import core.object.DoProperty.PropertyDataType;


public abstract class do_ExpandableListView_MAbstract extends DoUIModule{

	protected do_ExpandableListView_MAbstract() throws Exception {
		super();
	}
	
	/**
	 * 初始化
	 */
	@Override
	public void onInit() throws Exception{
        super.onInit();
        //注册属性
		this.registProperty(new DoProperty("childTemplate", PropertyDataType.String, "", true));
		this.registProperty(new DoProperty("groupTemplate", PropertyDataType.String, "", true));
		this.registProperty(new DoProperty("childTemplates", PropertyDataType.String, "", true));
		this.registProperty(new DoProperty("groupTemplates", PropertyDataType.String, "", true));
		this.registProperty(new DoProperty("isShowbar", PropertyDataType.Bool, "true", true));
		this.registProperty(new DoProperty("selectedColor", PropertyDataType.String, "ffffff00", true));
		this.registProperty(new DoProperty("allExpanded", PropertyDataType.Bool, "false", true));
	}
}