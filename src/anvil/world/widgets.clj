(ns anvil.world.widgets
  (:require [anvil.component :refer [draw-gui-view]]
            [anvil.entity :as entity]
            [anvil.widgets :as widgets]
            [cdq.context :as world]
            [anvil.app :as app]
            [gdl.context :as c]
            [gdl.ui :refer [ui-actor] :as ui]))

(defn-impl world/widgets [{:keys [cdq.context/player-eid] :as c}]
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
   (ui-actor {:draw #(draw-gui-view (entity/state-obj @(:cdq.context/player-eid @app/state))
                                    c)})
   (widgets/player-message)])
