(ns cdq.stage
  (:require [gdl.utils :as utils]))

(def ^:private actors*
  '[(cdq.create.stage.dev-menu/create (cdq.create.stage.dev-menu.config/create))
    (cdq.create.stage.actionbar/create)
    (cdq.create.stage.hp-mana-bar/create)
    (cdq.create.stage.windows/create [(cdq.create.stage.entity-info-window/create)
                                      (cdq.widgets.inventory/create)])
    (cdq.create.stage.player-state/create)
    (cdq.create.stage.player-message/actor)])

(defn actors [context]
  (map (fn [fn-invoc]
         (utils/req-resolve-call fn-invoc context))
       actors*))
