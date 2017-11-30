(ns org-z-me.core
    (:require [reagent.core :as r :refer [atom]]
              [re-frame.core :refer [subscribe dispatch dispatch-sync]]
              [org-z-me.handlers]
              [org-z-me.subs]
              [cljs-react-navigation.reagent :refer [stack-navigator stack-screen]]))

(defonce env (.-env js/process))
(defonce Expo (js/require "expo"))
(defonce secure-store (.-SecureStore Expo))
(defonce ReactNative (js/require "react-native"))
(defonce ReactNavigation (js/require "react-navigation"))
(def RNSD (js/require "react-native-simple-dialogs"))
(def confirm-dialog (r/adapt-react-class (aget RNSD "ConfirmDialog")))
(def app-registry (.-AppRegistry ReactNative))
(defn rn-comp [name] (-> ReactNative (aget name) r/adapt-react-class))
(defonce text (rn-comp "Text"))
(defonce view (rn-comp "View"))
(defonce image (rn-comp "Image"))
(defonce touchable-highlight (rn-comp "TouchableOpacity"))
(defonce flat-list (rn-comp "FlatList"))
(defonce text-input (rn-comp "TextInput"))
(defonce Alert (.-Alert ReactNative))
(defn alert [title] (.alert Alert title))
(defonce done-icon (js/require "./assets/images/done.png"))

(defn clj->json [data] (str (.stringify js/JSON (clj->js data)) "\n"))
(defn json->clj [line] (js->clj (.parse js/JSON line) :keywordize-keys true))

(defn save-to-store [token-key token-value]
  (.catch (.then (.setItemAsync secure-store token-key (clj->json token-value) (clj->js {}))
                 #(println "Saved " token-key))
          #(println "Couldnot save, error >>" %)))

(defn get-from-store [token-key db-key]
  (.catch (.then (.getItemAsync secure-store token-key (clj->js {}))
                 (fn [val] (if (nil? val)
                             (println "NOT GOT")
                             (dispatch [:set-key-val db-key (json->clj val)]))))
          (fn [e] (println "Error in Retrieving from store" e))))

(defn delete-from-store [token-key]
  (.catch (.then (.deleteItemAsync secure-store token-key (clj->js {}))
                 #(println "Deleted " token-key))
          #(println "Couldnot get, error >>" %)))

(defn render-flat-list [[view navigate]]
  (fn [data]
    (let [{:keys [item index]} (js->clj data :keywordize-keys true)]
      (r/as-element (view (js->clj item) navigate)))))

(defn display-task [{:keys [parent id name] :as data} navigate]
  [view {:style {:flex-direction "row" :width "100%" :margin-bottom 10 :backgroundColor "#F1CF00" :padding 8 :z-index 2 :border-radius 2 :shadow-color "#000000" :shadow-opacity 0.3 :shadow-radius 1 :shadow-offset {:height 1 :width 0.3}}}
   [touchable-highlight {:style {:flex 1} :on-press #(navigate "Org" {:id id})}
    [text {:style {:font-size 17}} name]]
   [touchable-highlight {:on-press #(println "DONE")}
    [image {:source done-icon :style {:height 32 :width 32 :align-self "center" :margin-left 5}}]]])

(defn organizer [props]
  (fn [{:keys [screenProps navigation] :as props}]
    (let [{:keys [navigate goBack state]} navigation
          id (:id (:params state))
          dialog? (r/atom false)
          tasks (subscribe [:get-val :tasks])
          edited-task (r/atom "")]
      (fn [props]
        (println @tasks)
        [view {:flex 1 :style {:margin 10}}
         [confirm-dialog
          {:title "What's on your Mind?"
           :visible @dialog?
           :onTouchOutside #(reset! dialog? false)
           :positiveButton {:title "ADD"
                            :on-press (fn []
                                        (dispatch-sync [:set-key-val :tasks
                                                        (conj @tasks
                                                              {:id (if @tasks
                                                                     (inc (:id (last @tasks)))
                                                                     1)
                                                               :parent id
                                                               :name @edited-task})])
                                        (reset! dialog? false)
                                        (save-to-store "tasks" @tasks))}
           :negativeButton {:title "CANCEL"
                            :on-press #(reset! dialog? false)}}
          [view [text-input {:style {:font-size 16 :color "#3311ff"}
                             :placeholder "Enter your task!"
                             :returnKeyType "done"
                             :blurOnSubmit true
                             :on-change-text #(reset! edited-task %)
                             :multiline true}]]]
         (if-let [filtered (seq (filter #(when (= (:parent %) id) %) @tasks))]
           [flat-list {:keyExtractor (fn [_ i] i)
                       :data (clj->js filtered)
                       :renderItem (render-flat-list [display-task navigate])}]
           [text {:style {:flex 1 :text-align "center" :text-align-vertical "center" :color "#f00"}}
            "YOU DONT HAVE ANY TASKS HERE YET!\n CLICK ADD NEW TASK AND START WRITING YOUR TASKS"])
         [touchable-highlight {:style {:position "absolute" :bottom 0 :width "100%"
                                       :background-color "#f00" :padding 10 :border-radius 5}
                               :on-press #(reset! dialog? true)}
          [text {:style {:text-align "center" :font-weight "bold"}} "ADD NEW TASK"]]]))))

(def OrgStack (stack-navigator {:Org {:screen (stack-screen organizer {:title "Org-Z-mE" :headerBackTitle " "})}}
                               {:initialRoute "Org" :initialRouteParams {:id 0}}))

(defn app-root []
  (fn []
    ;;(delete-from-store "tasks")
    (when (= (.-NODE_ENV env) "development")
      (set! (.-REACT_NAV_LOGGING (.-env js/process)) true))
    (get-from-store "tasks" :tasks)
    [:> OrgStack {}]))

(defn init []
  (dispatch-sync [:initialize-db])
  (.registerComponent app-registry "main" #(r/reactify-component app-root)))
