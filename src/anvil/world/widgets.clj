(ns anvil.world.widgets
  (:require [anvil.component :refer [draw-gui-view]]
            [anvil.entity :as entity]
            [anvil.world :as world]
            [gdl.ui :refer [ui-actor] :as ui]))

(defn hp-mana-bar [])

(defn dev-menu [])

(defn action-bar [])

(defn inventory [])

(defn entity-info-window [])

(defn player-message [])

(defn-impl world/widgets []
  [(if dev-mode?
     (dev-menu)
     (ui-actor {}))
   (ui/table {:rows [[{:actor (action-bar)
                       :expand? true
                       :bottom? true}]]
              :id :action-bar-table
              :cell-defaults {:pad 2}
              :fill-parent? true})
   (hp-mana-bar)
   (ui/group {:id :windows
              :actors [(entity-info-window)
                       (inventory)]})
   (ui-actor {:draw #(draw-gui-view (entity/state-obj @world/player-eid))})
   (player-message)])
