(ns cdq.application
  (:require [cdq.application.create :as create]
            [cdq.application.dispose :as dispose]
            [cdq.application.render :as render]
            [cdq.application.resize :as resize]
            [clojure.application :as application]
            [com.badlogic.gdx.backends.lwjgl3.application :as lwjgl-application])
  (:gen-class))

(def ^:private state (atom nil))

(defn -main []
  (lwjgl-application/start!
   {:listener (reify application/Listener
                (create [_ context]
                  (reset! state (create/do! context)))
                (dispose [_]
                  (dispose/do! @state))
                (pause [_])
                (render [_]
                  (swap! state render/do!))
                (resize [_ width height]
                  (resize/do! @state width height))
                (resume [_]))
    :config {:title "Cyber Dungeon Quest"
             :windowed-mode {:width 1440
                             :height 900}
             :foreground-fps 60
             :mac {:glfw-async? true
                   :taskbar-icon "icon.png"}}}))

(defn post-runnable! [f]
  (clojure.application/post-runnable! (:ctx/app @state)
                                      (fn [] (f @state))))

(comment
 (require '[cdq.ctx :as ctx]
          '[cdq.db :as db])

 (post-runnable!
  (fn [ctx]
    (ctx/handle-txs! ctx
                     [[:tx/spawn-creature
                       {:position [35 73]
                        :creature-property (db/build (:ctx/db ctx) :creatures/dragon-red)
                        :components {:entity/fsm {:fsm :fsms/npc
                                                  :initial-state :npc-sleeping}
                                     :entity/faction :evil}}]])))
 )
