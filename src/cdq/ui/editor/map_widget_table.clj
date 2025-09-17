(ns cdq.ui.editor.map-widget-table)

(defprotocol MapWidgetTable
  (get-value [_ schemas]))
