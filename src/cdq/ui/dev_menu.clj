(ns cdq.ui.dev-menu
  (:require [cdq.ui.table :as table]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.touchable :as touchable]
            [cdq.ui.menu :as menu]
            [clojure.vis-ui.label :as vis-label]))

(defn create
  [{:keys [menus update-labels]}]
  (table/create
   {:rows [[{:actor (menu/create
                     {:menus menus
                      :update-labels update-labels})
             :expand-x? true
             :fill-x? true
             :colspan 1}]
           [{:actor (doto (vis-label/create "")
                      (actor/set-touchable! touchable/disabled))
             :expand? true
             :fill-x? true
             :fill-y? true}]]
    :fill-parent? true}))
