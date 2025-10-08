(ns cdq.application
  (:require [cdq.game :as game])
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (org.lwjgl.system Configuration))
  (:gen-class))

(def state (atom nil))

(defn -main []
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (Lwjgl3Application. (reify ApplicationListener
                        (create [_]
                          (reset! state (game/create!)))
                        (dispose [_]
                          (game/dispose! @state))
                        (render [_]
                          (swap! state game/render!))
                        (resize [_ width height]
                          (game/resize! @state width height))
                        (pause [_])
                        (resume [_]))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle "Cyber Dungeon Quest")
                        (.setWindowedMode 1440 900)
                        (.setForegroundFPS 60))))

(comment

 (.postRunnable Gdx/app
                (fn []
                  (let [{:keys [ctx/db]
                         :as ctx} @state]
                    (txs/handle! ctx
                                 [[:tx/spawn-creature
                                   {:position [35 73]
                                    :creature-property (db/build db :creatures/dragon-red)
                                    :components {:entity/fsm {:fsm :fsms/npc
                                                              :initial-state :npc-sleeping}
                                                 :entity/faction :evil}}]]))))
 )
