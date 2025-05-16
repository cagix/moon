(ns cdq.game.application
  (:require [cdq.utils :as utils]
            [gdl.application :as application]))

(defn do! [{:keys [create
                   dispose
                   render
                   resize]
            :as opts}]
  (application/start! (reify application/Listener
                        (create! [_]
                          (utils/execute! create))

                        (dispose! [_]
                          (utils/execute! dispose))

                        (render! [_]
                          (utils/execute! render))

                        (resize! [_]
                          (utils/execute! resize)))
                      opts))
