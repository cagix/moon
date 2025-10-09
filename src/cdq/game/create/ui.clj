(ns cdq.game.create.ui
  (:require cdq.ui.actor-information
            cdq.ui.error-window
            [cdq.ui :as ui]))

(defn do! [ctx params]
  (ui/create! ctx params))
