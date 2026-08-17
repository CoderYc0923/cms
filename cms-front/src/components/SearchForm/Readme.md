# 自定义搜索表单控件

* 目前只支持输入框、普通选择框、时间选择框
* 用户通过传入数组 ```columns``` 来遍历出不同的数据录入控件以及控件对应的相关属性

## 数组search中对象字段介绍

| 字段名  | 介绍 | 是否必传 |
| --- | --- | --- |
| name | 表单项显示的标签文本  |  是  |
| field | 表单项的属性字段  |  是  |    |
| defaultValue | 表单项默认值  |  否  |
| mode | 控件类型 暂只支持('INPUT','SELECT','DATE_PICKER')这三个值  |  是  |
| custom | 控件对象的属性设置对象  |  否  |

## 控件配置项custom字段介绍
| 字段名  | 介绍 | 是否必传 |
| --- | --- | --- |
| placeholder | 控件内部的提示问题  |  否  |
| enum | 下拉选择框数组遍历对象  |  控件类型为SELECT时必传  |    |
| showAll | 下拉选择框是否额外显示全部选项 控件类型为SELECT时生效  |  否  |
| mode | 时间选择框类型 datePicker(天为粒度),weekPicker(周为粒度),monthPicker(月为粒度),rangePicker(时间范围选择)  |  控件类型为DATE_PICKER时必传  |
| showTime | 时间选择功能 mode为rangePicker时生效  |  否  |

## 事件
| 事件名称  | 介绍 | 是否必传 |
| --- | --- | --- |
| request |  通过该事件获取搜索表单的数据  |  是  |