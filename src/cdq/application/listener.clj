(ns cdq.application.listener)

(defn create
  [{:keys [state
           create
           dispose
           render
           resize]}]
  {:create! (fn []
              (reset! state (create)))
   :dispose! (fn []
               (dispose @state))
   :render! (fn []
              (swap! state render))
   :resize! (fn [width height]
              (resize @state width height))
   :pause! (fn [])
   :resume! (fn [])})
