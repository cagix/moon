(ns cdq.application
  (:require [cdq.utils :as utils]
            [gdl.application :as application]))

; TODO

; * startup slower -> maybe serialized require problem ?
; => beceause so many namespaces ?!

; * fix dev-menu start w. different world (assoc in config !?)
; => 'create' itself pass a config opts around ? idk.

; * click sometimes not working! ( stage & input click handler outside of stage is catched ?)
;  -> comment out stage & check
; => issue disappeared after restart (mac os problem?)

(defn- create-into! [initial-context create-fns]
  (reduce (fn [ctx create-fn]
            (if (vector? create-fn)
              (let [[k [f & params]] create-fn]
                (assoc ctx k (apply (requiring-resolve f) ctx params)))
              (do
               ((requiring-resolve create-fn) ctx)
               ctx)))
          initial-context
          create-fns))

(defn- create-initial-state [initial-context]
  (create-into! initial-context (concat (:ctx/create-app-state initial-context)
                                        (:ctx/create-game-state initial-context))))

(defn- reset-game-state [{:keys [ctx/create-game-state] :as ctx}]
  (create-into! ctx create-game-state))

(defn- render! [ctx render-fns]
  (reduce (fn [ctx render-fn]
            (if-let [result ((requiring-resolve render-fn) ctx)]
              result
              ctx))
          ctx
          render-fns))

(def state (atom nil))

(comment
 (spit "state.clj"
       (with-out-str
        (clojure.pprint/pprint
         (sort (keys @state)))))
 )

(defn reset-game-state! []
  (swap! state reset-game-state))

(defn -main []
  (let [{:keys [app-config
                initial-context
                dispose-fn
                resize-fn
                render-fns]} (utils/io-slurp-edn "cdq.application.edn")]
    (application/start!
     (utils/safe-merge app-config
                       {:create!
                        (fn []
                          (reset! state (create-initial-state initial-context)))

                        :dispose!
                        (fn []
                          ((requiring-resolve dispose-fn) @state))

                        :render!
                        (fn []
                          (swap! state render! render-fns))

                        :resize!
                        (fn [_width _height]
                          ((requiring-resolve resize-fn) @state))}))))
