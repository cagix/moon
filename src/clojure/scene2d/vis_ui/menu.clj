(ns clojure.scene2d.vis-ui.menu
  (:require [clojure.gdx.scene2d.actor :as actor]
            [clojure.scene2d.build.table :as btable]
            [clojure.scene2d.ui.table :as table]
            [clojure.scene2d.vis-ui.image :as image]
            [clojure.vis-ui.menu :as menu]
            [clojure.vis-ui.menu-bar :as menu-bar]
            [clojure.vis-ui.menu-item :as menu-item]
            [clojure.vis-ui.popup-menu :as popup-menu]
            [clojure.vis-ui.label :as label])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Cell
                                               Label)
           (com.badlogic.gdx.scenes.scene2d.utils ChangeListener)))

(defn- set-label-text-actor [label text-fn]
  (actor/create
   {:act (fn [this delta]
           (when-let [stage (actor/stage this)]
             (Label/.setText label (text-fn (.ctx stage)))))
    :draw (fn [this batch parent-alpha])}))

(defn- add-upd-label!
  ([table text-fn icon]
   (let [label (label/create "")
         sub-table (btable/create
                    {:rows [[{:actor (image/create {:image/object icon})}
                             label]]})]
     (.addActor table (set-label-text-actor label text-fn))
     (.expandX (Cell/.right (table/add! table sub-table)))))
  ([table text-fn]
   (let [label (label/create "")]
     (.addActor table (set-label-text-actor label text-fn))
     (.expandX (Cell/.right (table/add! table label))))))

(defn- add-update-labels! [menu-bar update-labels]
  (let [table (menu-bar/table menu-bar)]
    (doseq [{:keys [label update-fn icon]} update-labels]
      (let [update-fn #(str label ": " (update-fn %))]
        (if icon
          (add-upd-label! table update-fn icon)
          (add-upd-label! table update-fn))))))

(defn- add-menu! [menu-bar {:keys [label items]}]
  (let [app-menu (menu/create label)]
    (doseq [{:keys [label on-click]} items]
      (popup-menu/add-item! app-menu (doto (menu-item/create label)
                                       (.addListener (proxy [ChangeListener] []
                                                       (changed [event actor]
                                                         (when on-click
                                                           (on-click actor (.ctx (.getStage event))))))))))
    (menu-bar/add-menu! menu-bar app-menu)))

(defn create [{:keys [menus update-labels]}]
  (let [menu-bar (menu-bar/create)]
    (run! #(add-menu! menu-bar %) menus)
    (add-update-labels! menu-bar update-labels)
    (menu-bar/table menu-bar)))
