# moon.app

## -main


```
(defn -main []
  (let [config (-> "app.edn" io/resource slurp edn/read-string)]
    (load-components (:components config))
    (app/start (:lwjgl3 config)
               (app-listener (:app config)))))
```

=>

'config'

=>

load-components
