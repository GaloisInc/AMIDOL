port module App exposing (init, main)

import Browser
import Debug
import Dict
import Element exposing (..)
import Element.Background as Background
import Element.Border as Border
import Element.Events exposing (..)
import Element.Font as Font
import Element.Input as Input
import Html exposing (Html, div, img)
import Html.Attributes as HtmlAttr exposing (class, id, src)


main : Program () Model Msg
main =
    Browser.element
        { init = init
        , update = update
        , subscriptions = subscriptions
        , view = view
        }


port graphData : (String -> msg) -> Sub msg



-- MODEL


type alias Model =
    { title : String
    , graph : String
    , vars : Dict.Dict String String
    , newVar : String
    }


init : flags -> ( Model, Cmd Msg )
init flags =
    ( { title = "SIR"
      , graph = "" -- TODO: get from flags
      , vars =
            Dict.fromList
                [ ( "beta", "3.1415" )
                , ( "gamma", "42" )
                , ( "N", "9000" )
                ]
      , newVar = ""
      }
    , Cmd.none
    )



-- UPDATE


type Msg
    = GraphData String
    | ChangeTitle String
    | AddVar String
    | DeleteVar String
    | ChangeVar String String
    | ChangeNewVar String


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GraphData data ->
            ( Debug.log "GraphData" { model | graph = data }, Cmd.none )

        ChangeTitle newTitle ->
            ( { model | title = newTitle }, Cmd.none )

        AddVar key ->
            ( { model | vars = Dict.insert key "" model.vars, newVar = "" }, Cmd.none )

        DeleteVar key ->
            ( { model | vars = Dict.remove key model.vars }, Cmd.none )

        ChangeVar key value ->
            ( { model | vars = Dict.insert key value model.vars }, Cmd.none )

        ChangeNewVar newVarName ->
            ( { model | newVar = newVarName }, Cmd.none )



-- SUBSCRIPTIONS


subscriptions : Model -> Sub Msg
subscriptions model =
    graphData GraphData



-- VIEW


header : String -> Element Msg
header title =
    row
        [ width fill
        , paddingXY 20 5
        , Border.widthEach { bottom = 2, top = 0, left = 0, right = 0 }
        , Border.color <| rgb255 200 200 200
        ]
        [ Input.text [ centerX, width <| px 250 ]
            { label =
                Input.labelLeft
                    [ Font.color <| rgb255 160 160 160
                    , centerY
                    ]
                <|
                    text "Model:"
            , onChange = ChangeTitle
            , placeholder = Nothing
            , text = title
            }
        ]


sidebar : Dict.Dict String String -> String -> Element Msg
sidebar variables newVar =
    let
        varEl ( key, value ) =
            row
                [ spacing 10, padding 10, width fill ]
                [ Input.text
                    [ alignRight, width <| px 100 ]
                    { label = Input.labelLeft [ centerY ] <| text <| key ++ " ="
                    , onChange = ChangeVar key
                    , placeholder = Nothing
                    , text = value
                    }
                , Input.button
                    []
                    -- paddingEach { top = 0, right = 10, bottom = 0, left = 0 } ]
                    { onPress = Just (DeleteVar key)
                    , label = el [ Font.color <| rgb255 160 160 160 ] <| text "⨯"
                    }
                ]

        title =
            el
                [ centerX
                , paddingXY 0 20
                , Font.color <| rgb255 160 160 160
                ]
            <|
                text "Variables:"

        adder =
            row
                [ spacing 20, padding 20, width fill ]
                [ Input.text
                    [ alignRight, width fill ]
                    { label = Input.labelRight [] <| none
                    , onChange = ChangeNewVar
                    , placeholder = Nothing
                    , text = newVar
                    }
                , Input.button
                    [ width <| px 100 ]
                    { onPress =
                        if newVar == "" then
                            Nothing

                        else
                            Just (AddVar newVar)
                    , label =
                        el
                            [ Font.color <| rgb255 160 160 160
                            , Font.size 30
                            ]
                        <|
                            text "+"
                    }
                ]
    in
    column
        [ height fill
        , width <| fillPortion 1

        -- , spacingXY 0 10
        , Border.widthEach { bottom = 0, top = 0, left = 2, right = 0 }
        , Border.color <| rgb255 200 200 200
        ]
    <|
        [ title ]
            ++ (List.map varEl <| Dict.toList variables)
            ++ [ adder ]


exposedDiv : String -> List (Attribute msg) -> List (Html msg) -> Element msg
exposedDiv id attrs children =
    el attrs <| html <| div [ HtmlAttr.id id ] children


graphPanel : Element Msg
graphPanel =
    let
        palette =
            exposedDiv "palette"
                []
                [ img
                    [ HtmlAttr.id "redo_button"
                    , HtmlAttr.class "undo-redo"
                    , HtmlAttr.src "images/redo.png"
                    ]
                    []
                , img
                    [ HtmlAttr.id "undo_button"
                    , HtmlAttr.class "undo-redo"
                    , HtmlAttr.src "images/undo.png"
                    ]
                    []
                , div
                    [ HtmlAttr.class "spacer" ]
                    []
                , img
                    [ HtmlAttr.src "images/person.png"
                    , HtmlAttr.class "palette-img"
                    ]
                    []
                , img
                    [ HtmlAttr.src "images/sick.jpg"
                    , HtmlAttr.class "palette-img"
                    ]
                    []
                , img
                    [ HtmlAttr.src "images/happy.png"
                    , HtmlAttr.class "palette-img"
                    ]
                    []
                ]
    in
    el [ height fill, width <| fillPortion 4 ] <|
        column [ height fill, width fill ]
            [ palette
            , exposedDiv "graph" [ height <| px 600 ] []
            ]


view : Model -> Html Msg
view model =
    layout [ height fill ] <|
        column [ height fill, width fill ]
            [ header model.title
            , row [ height fill, width fill ]
                [ graphPanel, sidebar model.vars model.newVar ]
            ]
