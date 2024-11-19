(ns moon.screens.editor
  (:require [clojure.string :as str]
            [gdl.db :as db]
            [gdl.input :refer [key-just-pressed?]]
            [gdl.property :as property]
            [gdl.screen :as screen]
            [gdl.ui :as ui]
            [moon.core :refer [add-actor]]
            [moon.editor.property :as widgets.property]
            [moon.editor.overview :as properties-overview]
            [moon.widgets.background-image :as background-image])
  (:import (com.kotcrab.vis.ui.widget.tabbedpane Tab TabbedPane TabbedPaneAdapter)))

; FIXME overview table not refreshed after changes in properties

(defn- edit-property [id]
  (add-actor (widgets.property/editor-window (db/get-raw id))))

(defn- property-type-tabs []
  (for [property-type (sort (property/types))]
    {:title (str/capitalize (name property-type))
     :content (properties-overview/table property-type edit-property)}))

(defn- tab-widget [{:keys [title content savable? closable-by-user?]}]
  (proxy [Tab] [(boolean savable?) (boolean closable-by-user?)]
    (getTabTitle [] title)
    (getContentTable [] content)))

(defn- tabs-table []
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
    table))

(defn create []
  {:actors [(background-image/create)
            (tabs-table)
            (ui/actor {:act (fn []
                              (when (key-just-pressed? :shift-left)
                                (screen/change :screens/main-menu)))})]})
