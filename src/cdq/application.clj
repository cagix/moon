(ns cdq.application)

(defn- render!* [ctx f]
  (let [result (if (vector? f)
                 (let [[f params] f]
                   ((requiring-resolve f) ctx params))
                 ((requiring-resolve f) ctx))]
    (if (nil? result)
      ctx
      result)))

(def state (atom nil))

(defn listener
  [{:keys [initial-record
           create-pipeline
           dispose-fn
           render-pipeline
           resize-fn]}]
  (let [create! (fn []
                  (reduce render!*
                          ((requiring-resolve initial-record))
                          create-pipeline))
        dispose! (fn [ctx]
                   (requiring-resolve dispose-fn) ctx)
        render! (fn [ctx]
                  (reduce render!* ctx render-pipeline))
        resize! (fn [ctx width height]
                  ((requiring-resolve resize-fn) ctx width height))]
    {:create! (fn []
                (reset! state (create!)))
     :dispose! (fn []
                 (dispose! @state))
     :render! (fn []
                (swap! state render!))
     :resize! (fn [width height]
                (resize! @state width height))
     :pause! (fn [])
     :resume! (fn [])}))
