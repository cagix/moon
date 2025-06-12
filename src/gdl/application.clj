(ns gdl.application
  (:require [gdx.backends.lwjgl.application :as application]
            [gdx.utils.shared-library-loader :as shared-library-loader]))

(defn execute! [[f params]]
  (f params))

(defn provide-impl-namespace
  ([[atype implementation-ns protocol]]
   (let [protocol @(requiring-resolve protocol)
         method-map (update-vals (:sigs protocol)
                                 (fn [{:keys [name]}]
                                   (requiring-resolve (symbol (str implementation-ns "/" name)))))]
     (extend atype protocol method-map)))
  ([impls]
   (run! provide-impl-namespace impls)))

(defn start! [os-config lwjgl3-config listener]
  (provide-impl-namespace [com.badlogic.gdx.Application      'gdx.app              'gdl.app/Application            ]
                          [com.badlogic.gdx.Files            'gdx.files            'gdl.files/Files                ]
                          [com.badlogic.gdx.Input            'gdx.input            'gdl.input/Input                ]
                          [com.badlogic.gdx.utils.Disposable 'gdx.utils.disposable 'gdl.utils.disposable/Disposable])
  (run! execute! (get os-config (shared-library-loader/operating-system)))
  (application/start! lwjgl3-config listener))
