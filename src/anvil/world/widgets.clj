(ns anvil.world.widgets
  (:require [anvil.component :refer [draw-gui-view]]
            [anvil.entity :as entity]
            [anvil.widgets :as widgets]
            [anvil.world :as world]
            [gdl.context :as c]
            [gdl.ui :refer [ui-actor] :as ui]))

(defn-impl world/widgets [c]
  [(if dev-mode?
     (widgets/dev-menu c)
     (ui-actor {}))
   (ui/table {:rows [[{:actor (widgets/action-bar)
                       :expand? true
                       :bottom? true}]]
              :id :action-bar-table
              :cell-defaults {:pad 2}
              :fill-parent? true})
   (widgets/hp-mana-bar c)
   (ui/group {:id :windows
              :actors [(widgets/entity-info-window c)
                       (widgets/inventory c)]})
   (ui-actor {:draw #(draw-gui-view (entity/state-obj @world/player-eid)
                                    c)})
   (widgets/player-message)])
