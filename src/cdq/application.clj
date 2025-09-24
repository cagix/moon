(ns cdq.application
  (:require [clojure.application :as application]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [com.badlogic.gdx.backends.lwjgl3.application :as lwjgl-application])
  (:gen-class))

(def ^:private state (atom nil))

(defn -main []
  (let [{:keys [listener
                config]} (-> "cdq.application.edn"
                             io/resource
                             slurp
                             edn/read-string)]
    (lwjgl-application/start!
     {:listener (let [{:keys [
                              dispose
                              render
                              resize]} (update-vals listener requiring-resolve)]
                  (reify application/Listener
                    (create [_ context]
                      (reset! state (reduce (fn [ctx f]
                                              (f ctx))
                                            context
                                            (map requiring-resolve
                                                 '[cdq.application.create.record/do!
                                                   cdq.application.create.validation/do!
                                                   cdq.application.create.editor/do!
                                                   cdq.application.create.handle-txs/do!
                                                   cdq.application.create.db/do!
                                                   cdq.application.create.vis-ui/do!
                                                   cdq.application.create.graphics/do!
                                                   cdq.application.create.stage/do!
                                                   cdq.application.create.input/do!
                                                   cdq.application.create.audio/do!
                                                   cdq.application.create.remove-files/do!
                                                   cdq.application.create.world/do!
                                                   cdq.application.create.reset-game-state/do!]))))
                    (dispose [_]
                      (dispose @state))
                    (pause [_])
                    (render [_]
                      (swap! state render))
                    (resize [_ width height]
                      (resize @state width height))
                    (resume [_])))
      :config config})))

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
