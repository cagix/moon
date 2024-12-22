(ns anvil.world.widgets
  (:require [anvil.component :refer [draw-gui-view]]
            [anvil.entity :as entity]
            [anvil.widgets :as widgets]
            [anvil.world :as world]
            [gdl.ui :refer [ui-actor] :as ui]))

(defn-impl world/widgets []
  [(if dev-mode?
     (widgets/dev-menu)
     (ui-actor {}))
   (ui/table {:rows [[{:actor (widgets/action-bar)
                       :expand? true
                       :bottom? true}]]
              :id :action-bar-table
              :cell-defaults {:pad 2}
              :fill-parent? true})
   (widgets/hp-mana-bar)
   (ui/group {:id :windows
              :actors [(widgets/entity-info-window)
                       (widgets/inventory)]})
   (ui-actor {:draw #(draw-gui-view (entity/state-obj @world/player-eid))})
   (widgets/player-message)])
