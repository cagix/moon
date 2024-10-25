(ns moon.world.widgets
  (:require [gdl.ui :as ui]
            [gdl.utils :refer [dev-mode?]]
            [moon.component :refer [defc] :as component]))

(defc :world/widgets
  (component/create [_]
    [(if dev-mode?
       (component/create [:widgets/dev-menu nil])
       (ui/actor {}))
     (ui/table {:rows [[{:actor (component/create [:widgets/action-bar nil])
                         :expand? true
                         :bottom? true}]]
                :id :action-bar-table
                :cell-defaults {:pad 2}
                :fill-parent? true})
     (component/create [:widgets/hp-mana nil])
     (ui/group {:id :windows
                :actors [(component/create [:widgets/entity-info-window nil])
                         (component/create [:widgets/inventory          nil])]})
     (component/create [:widgets/draw-item-on-cursor nil])
     (component/create [:widgets/player-message      nil])]))
