(ns cdq.application.listener)

(defn create
  [{:keys [create
           dispose
           render
           resize
           atom-var]}]
  (let [state @atom-var
        [create-fn create-params] create
        ]
    {:create! (fn [gdx]
                (swap! state (fn [ctx]
                               (-> ctx
                                   (assoc :ctx/gdx gdx)
                                   (create-fn create-params)))))
     :dispose! (fn []
                 (swap! state dispose))
     :render! (fn []
                (swap! state render))
     :resize! (fn [width height]
                (swap! state resize width height))
     :pause! (fn [])
     :resume! (fn [])}))
