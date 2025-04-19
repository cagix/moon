(ns cdq.create.stage.dev-menu
  (:require cdq.game
            cdq.graphics.sprite
            [clojure.gdx.scenes.scene2d.group :refer [add-actor!]]
            [cdq.ui :as ui :refer [ui-actor]])
  (:import (com.badlogic.gdx.scenes.scene2d Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Label Table)
           (com.kotcrab.vis.ui.widget PopupMenu)))

(defn- menu-item [text on-clicked]
  (doto (ui/menu-item text)
    (.addListener (ui/change-listener on-clicked))))

(defn- set-label-text-fn [label text-fn]
  (fn [context]
    (Label/.setText label (str (text-fn context)))))

(defn- add-upd-label
  ([c table text-fn icon]
   (let [icon (ui/image->widget (cdq.graphics.sprite/create c icon) {})
         label (ui/label "")
         sub-table (ui/table {:rows [[icon label]]})]
     (add-actor! table (ui-actor {:act (set-label-text-fn label text-fn)}))
     (.expandX (.right (Table/.add table sub-table)))))
  ([c table text-fn]
   (let [label (ui/label "")]
     (add-actor! table (ui-actor {:act (set-label-text-fn label text-fn)}))
     (.expandX (.right (Table/.add table label))))))

(defn- add-update-labels [c menu-bar update-labels]
  (let [table (ui/menu-bar->table menu-bar)]
    (doseq [{:keys [label update-fn icon]} update-labels]
      (let [update-fn #(str label ": " (update-fn %))]
        (if icon
          (add-upd-label c table update-fn icon)
          (add-upd-label c table update-fn))))))

(defn- add-menu [c menu-bar {:keys [label items]}]
  (let [app-menu (ui/menu label)]
    (doseq [{:keys [label on-click]} items]
      (PopupMenu/.addItem app-menu (menu-item label (if on-click
                                                      #(on-click @cdq.game/state) ;=> change-listener get .application-state @ ui but not sure if it has that or go through actor
                                                      (fn [])))))
    (ui/add-menu menu-bar app-menu)))

(defn- create-menu-bar [c menus]
  (let [menu-bar (ui/menu-bar)]
    (run! #(add-menu c menu-bar %) menus)
    menu-bar))

(defn- dev-menu* [c {:keys [menus update-labels]}]
  (let [menu-bar (create-menu-bar c menus)]
    (add-update-labels c menu-bar update-labels)
    menu-bar))

(defn table [c config]
  (ui/table {:rows [[{:actor (ui/menu-bar->table (dev-menu* c config))
                      :expand-x? true
                      :fill-x? true
                      :colspan 1}]
                    [{:actor (doto (ui/label "")
                               (.setTouchable Touchable/disabled))
                      :expand? true
                      :fill-x? true
                      :fill-y? true}]]
             :fill-parent? true}))

(defn create [dev-menu-config context]
  (table context (clojure.utils/req-resolve-call dev-menu-config context)))
