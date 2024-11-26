(ns ^:no-doc app.screens.editor
  (:require [clojure.string :as str]
            [forge.input :refer [key-just-pressed?]]
            [forge.ui :as ui]
            [forge.graphics :refer [add-actor change-screen]]
            [forge.db :as db]
            [forge.editor.property :as widgets.property]
            [forge.editor.overview :as properties-overview]
            [forge.property :as property]
            [moon.widgets.background-image :as background-image])
  (:import (com.kotcrab.vis.ui.widget.tabbedpane Tab TabbedPane TabbedPaneAdapter)))

; FIXME overview table not refreshed after changes in properties

(defn- edit-property [id]
  (add-actor (widgets.property/editor-window (db/get-raw id))))

(defn- property-type-tabs []
  (for [property-type (sort (db/property-types))]
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
                                (change-screen :screens/main-menu)))})]})
