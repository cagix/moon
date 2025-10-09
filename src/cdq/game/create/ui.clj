(ns cdq.game.create.ui
  (:require cdq.scene2d.build.map-widget-table
            clojure.scene2d.build.group
            clojure.scene2d.build.horizontal-group
            clojure.scene2d.build.scroll-pane
            clojure.scene2d.build.separator-horizontal
            clojure.scene2d.build.separator-vertical
            clojure.scene2d.build.stack
            cdq.ui.actor-information
            cdq.ui.error-window
            [cdq.ui :as ui]))

(defn do! [ctx params]
  (ui/create! ctx params))
