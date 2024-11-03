
(comment
 (require '[moon.malli :as malli])
 (doseq [k (malli/map-keys failed-schema)]
   (try (-> k schema/of schema/form m/schema)
        (catch Throwable t
          (println k " - " (-> k schema/of schema/form))))))

(comment
 (= pos? (schema/form (schema/of :stats/movement-speed)))
 ; false
 (.toString pos?)
 "clojure.core$pos_QMARK_@7a2f772a"
 (.toString (schema/form (schema/of :stats/movement-speed)))
 "clojure.core$pos_QMARK_@4d04ce06"

 ; https://github.com/metosin/malli/issues/556
 (let [schema (schema/form (schema/of :stats/movement-speed))]
   (m/schema schema)
   )
 )

(defn validate! [property]
  (let [m-schema (try (m/schema (m-schema property))
                      (catch clojure.lang.ExceptionInfo e
                        (def failed-schema (m-schema property))
                        (throw (ex-info "m/schema fail"
                                        {:property property
                                         :m-schema (m-schema property)}
                                        e))))]
    (when-not (m/validate m-schema property)
      (throw (invalid-ex-info m-schema property)))))


; Again had problems with pos? and widget-type
; => use keywords
