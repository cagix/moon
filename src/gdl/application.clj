(ns gdl.application)

(defn provide-impl-namespace [impls]
  (doseq [[atype implementation-ns protocol] impls]
    (let [protocol @(requiring-resolve protocol)
          method-map (update-vals (:sigs protocol)
                                  (fn [{:keys [name]}]
                                    (requiring-resolve (symbol (str implementation-ns "/" name)))))]
      (extend atype protocol method-map))))

(defn impl-gdx [_params]
  (provide-impl-namespace [[com.badlogic.gdx.Application      'gdx.app              'gdl.app/Application            ]
                           [com.badlogic.gdx.Files            'gdx.files            'gdl.files/Files                ]
                           [com.badlogic.gdx.Input            'gdx.input            'gdl.input/Input                ]
                           [com.badlogic.gdx.utils.Disposable 'gdx.utils.disposable 'gdl.utils.disposable/Disposable]]))
