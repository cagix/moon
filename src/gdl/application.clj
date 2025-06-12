(ns gdl.application
  (:require [gdx.backends.lwjgl.application :as application]
            [gdx.utils.shared-library-loader :as shared-library-loader]))

(defn- execute! [[f params]]
  (f params))

(defn provide-impl-namespace [atype protocol implementation-ns]
  (let [protocol @(requiring-resolve protocol)
        method-map (update-vals (:sigs protocol)
                                (fn [{:keys [name]}]
                                  (requiring-resolve (symbol (str implementation-ns "/" name)))))]
    (extend atype protocol method-map)))

(defn start! [os-config lwjgl3-config listener]
  (provide-impl-namespace com.badlogic.gdx.Application      'gdl.app/Application                 'gdx.app)
  (provide-impl-namespace com.badlogic.gdx.Files            'gdl.files/Files                     'gdx.files)
  (provide-impl-namespace com.badlogic.gdx.Input            'gdl.input/Input                     'gdx.input)
  (provide-impl-namespace com.badlogic.gdx.utils.Disposable 'gdl.utils.disposable/Disposable     'gdx.utils.disposable)
  (run! execute! (get os-config (shared-library-loader/operating-system)))
  (application/start! lwjgl3-config listener))



