(ns cdq.start.gdx-app
  (:require cdq.application
            cdq.gdx.backends.lwjgl))

(defn do! [{:keys [lwjgl-app-config
                   create!
                   dispose!
                   render!
                   resize!]}]
  (cdq.gdx.backends.lwjgl/start-application!
   lwjgl-app-config
   {:create! (fn [gdx]
               (reset! cdq.application/state
                       (let [[f config] create!]
                         ((requiring-resolve f) gdx config))))
    :dispose! (fn []
                ((requiring-resolve dispose!) @cdq.application/state))
    :render! (fn []
               (swap! cdq.application/state (fn [ctx]
                                              (reduce (fn [ctx f]
                                                        (let [result (if (vector? f)
                                                                       (let [[f params] f]
                                                                         ((requiring-resolve f) ctx params))
                                                                       ((requiring-resolve f) ctx))]
                                                          (if (nil? result)
                                                            ctx
                                                            result)))
                                                      ctx
                                                      render!))))
    :resize! (fn [width height]
               ((requiring-resolve resize!) @cdq.application/state width height))}))
