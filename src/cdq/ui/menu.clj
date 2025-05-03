(ns cdq.ui.menu
  (:require cdq.application
            [cdq.graphics.sprite :as sprite]
            [cdq.ui :as ui :refer [ui-actor]]
            [cdq.ui.group :refer [add-actor!]])
  (:import (com.badlogic.gdx.scenes.scene2d Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Label Table)
           (com.kotcrab.vis.ui.widget Menu MenuBar MenuItem PopupMenu)))

(defn- set-label-text-fn [label text-fn]
  (fn [context]
    (Label/.setText label (str (text-fn context)))))

(defn- add-upd-label!
  ([c table text-fn icon]
   (let [icon (ui/image->widget (sprite/create c icon) {})
         label (ui/label "")
         sub-table (ui/table {:rows [[icon label]]})]
     (add-actor! table (ui-actor {:act (set-label-text-fn label text-fn)}))
     (.expandX (.right (Table/.add table sub-table)))))
  ([c table text-fn]
   (let [label (ui/label "")]
     (add-actor! table (ui-actor {:act (set-label-text-fn label text-fn)}))
     (.expandX (.right (Table/.add table label))))))

(defn- add-update-labels! [c menu-bar update-labels]
  (let [table (MenuBar/.getTable menu-bar)]
    (doseq [{:keys [label update-fn icon]} update-labels]
      (let [update-fn #(str label ": " (update-fn %))]
        (if icon
          (add-upd-label! c table update-fn icon)
          (add-upd-label! c table update-fn))))))

(defn- add-menu! [c menu-bar {:keys [label items]}]
  (let [app-menu (Menu. label)]
    (doseq [{:keys [label on-click]} items]
      (PopupMenu/.addItem app-menu (doto (MenuItem. label)
                                     (.addListener (ui/change-listener (if on-click
                                                                         #(on-click @cdq.application/state) ;=> change-listener get .application-state @ ui but not sure if it has that or go through actor
                                                                         (fn [])))))))
    (MenuBar/.addMenu menu-bar app-menu)))

(defn create [context {:keys [menus update-labels]}]
  (ui/table {:rows [[{:actor (let [menu-bar (MenuBar.)]
                               (run! #(add-menu! context menu-bar %) menus)
                               (add-update-labels! context menu-bar update-labels)
                               (MenuBar/.getTable menu-bar))
                      :expand-x? true
                      :fill-x? true
                      :colspan 1}]
                    [{:actor (doto (ui/label "")
                               (.setTouchable Touchable/disabled))
                      :expand? true
                      :fill-x? true
                      :fill-y? true}]]
             :fill-parent? true}))
