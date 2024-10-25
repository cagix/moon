(ns ^:no-doc moon.editor.visui
  (:require [gdl.ui :as ui]
            [moon.component :as component :refer [defc]]
            [moon.db :as db]
            [moon.property :as property]
            [moon.stage :as stage])
  (:import (com.kotcrab.vis.ui.widget.tabbedpane Tab TabbedPane TabbedPaneAdapter)))

; FIXME overview table not refreshed after changes in properties

(defn- edit-property [id]
  (stage/add! (component/create [:widgets/property-editor (db/get-raw id)])))

(defn- property-type-tabs []
  (for [property-type (sort (property/types))]
    {:title (:title (property/overview property-type))
     :content (component/create [:widgets/properties-overview property-type edit-property])}))

(defn- tab-widget [{:keys [title content savable? closable-by-user?]}]
  (proxy [Tab] [(boolean savable?) (boolean closable-by-user?)]
    (getTabTitle [] title)
    (getContentTable [] content)))

(defc :editor/main-table
  (component/create [_]
    (let [table (ui/table {:fill-parent? true})
          container (ui/table {})
          tabbed-pane (TabbedPane.)]
      (.addListener tabbed-pane
                    (proxy [TabbedPaneAdapter] []
                      (switchedTab [^Tab tab]
                        (.clearChildren container)
                        (.fill (.expand (.add container (.getContentTable tab)))))))
      (.fillX (.expandX (.add table (.getTable tabbed-pane))))
      (.row table)
      (.fill (.expand (.add table container)))
      (.row table)
      (.pad (.left (.add table (ui/label "[LIGHT_GRAY]Left-Shift: Back to Main Menu[]"))) (float 10))
      (doseq [tab-data (property-type-tabs)]
        (.add tabbed-pane (tab-widget tab-data)))
      table)))
