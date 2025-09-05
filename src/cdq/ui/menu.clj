(ns cdq.ui.menu
  (:require [cdq.ui.group :as group]
            [cdq.ui :as ui]
            [cdq.ui.image :as image]
            [cdq.ui.utils :as utils])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)
           (com.badlogic.gdx.scenes.scene2d.ui Label
                                               Table)
           (com.kotcrab.vis.ui.widget Menu
                                      MenuBar
                                      MenuItem
                                      PopupMenu)))

(defn- set-label-text-actor [label text-fn]
  {:actor/type :actor.type/actor
   :act (fn [_this _delta ctx]
          (Label/.setText label (str (text-fn ctx))))})

(defn- add-upd-label!
  ([table text-fn icon]
   (let [icon (image/create icon {})
         label (ui/label {:label/text ""})
         sub-table (ui/table {:rows [[icon label]]})]
     (group/add! table (set-label-text-actor label text-fn))
     (.expandX (.right (Table/.add table sub-table)))))
  ([table text-fn]
   (let [label (ui/label {:label/text ""})]
     (group/add! table (set-label-text-actor label text-fn))
     (.expandX (.right (Table/.add table label))))))

(defn- add-update-labels! [menu-bar update-labels]
  (let [table (MenuBar/.getTable menu-bar)]
    (doseq [{:keys [label update-fn icon]} update-labels]
      (let [update-fn #(str label ": " (update-fn %))]
        (if icon
          (add-upd-label! table update-fn icon)
          (add-upd-label! table update-fn))))))

(defn- add-menu! [menu-bar {:keys [label items]}]
  (let [app-menu (Menu. label)]
    (doseq [{:keys [label on-click]} items]
      (PopupMenu/.addItem app-menu (doto (MenuItem. label)
                                     (.addListener (utils/change-listener
                                                    (fn [actor ctx]
                                                      (when on-click
                                                        (on-click actor ctx))))))))
    (MenuBar/.addMenu menu-bar app-menu)))

(import 'com.kotcrab.vis.ui.VisUI)
(import 'com.kotcrab.vis.ui.widget.MenuBar$MenuBarStyle)

(defn ->MenuBar []
  (MenuBar. ^MenuBar$MenuBarStyle
            (.get (VisUI/getSkin) "default", MenuBar$MenuBarStyle)))

(defn create [{:keys [menus update-labels]}]
  {:actor/type :actor.type/table
   :rows [[{:actor (let [menu-bar (->MenuBar)]
                     (run! #(add-menu! menu-bar %) menus)
                     (add-update-labels! menu-bar update-labels)
                     (MenuBar/.getTable menu-bar))
            :expand-x? true
            :fill-x? true
            :colspan 1}]
          [{:actor {:actor/type :actor.type/label
                    :label/text ""
                    :actor/touchable :disabled}
            :expand? true
            :fill-x? true
            :fill-y? true}]]
   :fill-parent? true})
