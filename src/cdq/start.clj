(ns cdq.start
  (:require [cdq.application.unit-scale]
            [cdq.application.world-unit-scale]
            [cdq.application.register-self]
            [cdq.application.colors :as colors]
            [cdq.application.config :as config]
            [cdq.application.context-record :as context-record]
            [cdq.application.draw-on-world-viewport :as draw-on-world-viewport]
            [cdq.application.draw-impl :as draw-impl]
            [cdq.application.editor-widgets]
            [cdq.application.effects :as effects]
            [cdq.application.entity-components :as entity-components]
            [cdq.application.entity-states]
            [cdq.application.entity-tick]
            [cdq.application.extend-scene2d]
            [cdq.application.fsms :as fsms]
            [cdq.application.info :as application.info]
            [cdq.application.lwjgl :as lwjgl]
            [cdq.application.render-layers :as render-layers]
            [cdq.application.txs]
            [cdq.application.tx-spawn-schema :as tx-spawn-schema]
            [cdq.application.os-settings :as os-settings]
            [cdq.application.ui-actors :as ui-actors]
            [cdq.application.db])
  (:gen-class))

(defn -main []
  (reduce (fn [ctx f]
            (f ctx))
          (config/load "ctx.edn")
          [context-record/create
           effects/init!
           cdq.application.register-self/do!
           fsms/create
           entity-components/create
           tx-spawn-schema/create
           ui-actors/create
           draw-on-world-viewport/create
           draw-impl/create
           application.info/create
           cdq.application.unit-scale/create
           cdq.application.world-unit-scale/create
           cdq.application.db/create2
           render-layers/create
           os-settings/handle!
           colors/define-gdx-colors!
           cdq.application.txs/extend-it
           cdq.application.extend-scene2d/extend-it
           lwjgl/start-gdx-app]))
