(ns moon.widgets.dev-menu
  (:require [forge.ui :as ui]
            [forge.app :refer [image]])
  (:import (com.kotcrab.vis.ui.widget Menu MenuItem MenuBar)))

(defn- menu-item [text on-clicked]
  (doto (MenuItem. text)
    (.addListener (ui/change-listener on-clicked))))

(defn- add-upd-label
  ([table text-fn icon]
   (let [icon (ui/image->widget (image icon) {})
         label (ui/label "")
         sub-table (ui/table {:rows [[icon label]]})]
     (.addActor table (ui/actor {:act #(.setText label (text-fn))}))
     (.expandX (.right (.add table sub-table)))))
  ([table text-fn]
   (let [label (ui/label "")]
     (.addActor table (ui/actor {:act #(.setText label (text-fn))}))
     (.expandX (.right (.add table label))))))

(defn- add-update-labels [menu-bar update-labels]
  (let [table (.getTable menu-bar)]
    (doseq [{:keys [label update-fn icon]} update-labels]
      (let [update-fn #(str label ": " (update-fn))]
        (if icon
          (add-upd-label table update-fn icon)
          (add-upd-label table update-fn))))))

(defn- add-menu [menu-bar {:keys [label items]}]
  (let [app-menu (Menu. label)]
    (doseq [{:keys [label on-click]} items]
      (.addItem app-menu (menu-item label (or on-click (fn [])))))
    (.addMenu menu-bar app-menu)))

(defn- create-menu-bar [menus]
  (let [menu-bar (MenuBar.)]
    (run! #(add-menu menu-bar %) menus)
    menu-bar))

(defn create [{:keys [menus update-labels]}]
  (let [menu-bar (create-menu-bar menus)]
    (add-update-labels menu-bar update-labels)
    menu-bar))
