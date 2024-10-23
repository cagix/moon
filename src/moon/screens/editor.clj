(ns ^:no-doc moon.screens.editor
  (:require [gdl.input :refer [key-just-pressed?]]
            [moon.property :as property]
            [moon.editor.overview :refer [overview-table]]
            [moon.editor.visui :as editor]
            [gdl.ui :as ui]
            [moon.ui.stage-screen :as stage-screen :refer [stage-add!]]
            [moon.screen :as screen])
  (:import (com.kotcrab.vis.ui.widget.tabbedpane Tab TabbedPane TabbedPaneAdapter)))

(defn- edit-property [property-id]
  (stage-add! (editor/property-editor-window property-id)))

(defn- property-type-tabs []
  (for [property-type (sort (property/types))]
    {:title (:title (property/overview property-type))
     :content (overview-table property-type edit-property)}))

(defn- tab-widget [{:keys [title content savable? closable-by-user?]}]
  (proxy [Tab] [(boolean savable?) (boolean closable-by-user?)]
    (getTabTitle [] title)
    (getContentTable [] content)))

(defn- main-table []
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

(defn screen [background-image]
  [:screens/property-editor
   (stage-screen/create :actors
                        [(background-image)
                         (main-table)
                         (ui/actor {:act (fn []
                                           (when (key-just-pressed? :shift-left)
                                             (screen/change! :screens/main-menu)))})])])
