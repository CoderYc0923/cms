/** Ant Design Vue ConfigProvider 主题，对齐 design system token */
export function createAntTheme () {
  return {
    token: {
      colorPrimary: '#4B5563',
      colorSuccess: '#00B42A',
      colorWarning: '#FF7D00',
      colorError: '#F53F3F',
      colorText: '#1D2129',
      colorTextSecondary: '#86909C',
      colorTextTertiary: '#C9CDD4',
      colorBorder: '#E5E6EB',
      colorBgContainer: '#FFFFFF',
      colorBgLayout: '#FAFBFC',
      colorBgTextHover: '#F2F3F5',
      borderRadius: 8,
      borderRadiusLG: 10,
      fontSize: 14,
      fontFamily:
        "'Inter', 'SF Pro Text', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif",
      controlHeight: 36,
      lineHeight: 1.5715,
      motionDurationMid: '0.15s'
    },
    components: {
      Table: {
        headerBg: 'transparent',
        headerColor: '#86909C',
        headerSplitColor: '#E5E6EB',
        rowHoverBg: '#F7F8FA',
        borderColor: '#E5E6EB',
        cellPaddingBlock: 14,
        cellPaddingInline: 16
      },
      Menu: {
        itemHeight: 36,
        itemMarginInline: 4,
        itemBorderRadius: 6,
        itemSelectedBg: 'rgba(75, 85, 99, 0.08)',
        itemSelectedColor: '#4B5563',
        itemHoverBg: '#F2F3F5',
        groupTitleColor: '#86909C',
        fontSize: 13
      },
      Modal: {
        borderRadiusLG: 10,
        boxShadow: '0 4px 24px rgba(0, 0, 0, 0.06)'
      },
      Button: {
        borderRadius: 8,
        controlHeight: 36,
        paddingInline: 16
      },
      Input: {
        borderRadius: 8,
        controlHeight: 36
      },
      Select: {
        borderRadius: 8,
        controlHeight: 36
      }
    }
  }
}
