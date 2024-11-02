(ns moon.world.widgets
  (:require [gdl.ui :as ui]
            [gdl.utils :refer [dev-mode?]]
            [moon.component :as component]
            [moon.entity :as entity]
            [moon.player :as player]
            [moon.widgets.windows :as windows]))

(defmethods :world/widgets
  (component/create [_]
    [(if dev-mode?
       (component/create [:widgets/dev-menu])
       (ui/actor {}))
     (ui/table {:rows [[{:actor (component/create [:widgets/action-bar])
                         :expand? true
                         :bottom? true}]]
                :id :action-bar-table
                :cell-defaults {:pad 2}
                :fill-parent? true})
     (component/create [:widgets/hp-mana])
     (windows/create)
     (ui/actor {:draw #(entity/draw-gui-view (entity/state-obj @player/eid))})
     (component/create [:widgets/player-message])]))
