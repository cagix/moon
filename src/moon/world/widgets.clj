(ns moon.world.widgets
  (:require [gdl.ui :as ui]
            [gdl.utils :refer [dev-mode?]]
            [moon.component :refer [defc] :as component]))

(defc :world/widgets
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
     (ui/group {:id :windows
                :actors [(component/create [:widgets/entity-info-window])
                         (component/create [:widgets/inventory])]})
     (component/create [:widgets/draw-item-on-cursor])
     (component/create [:widgets/player-message])]))
