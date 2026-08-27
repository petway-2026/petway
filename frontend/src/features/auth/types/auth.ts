export interface SignupRequest {
    email: string;
    password: string;
    nickname: string;
}

export interface LoginRequest {
    email: string;
    password: string;
}

export type UserRole =
    | "USER"
    | "ADMIN";

export interface User {
    userId: number;
    email: string;
    nickname: string;
    userRole: UserRole;
}

export interface SignupRequest {
    email: string;
    password: string;
    nickname: string;
}

export interface LoginRequest {
    email: string;
    password: string;
}

export interface LoginResponse {
    message: string;
    user: User;
}

export interface SignupResponse {
    userId: number;
    message: string;
}

export interface ApiError {
    message: string;
}

export interface LoginResponse {
    message: string;
    user: User;
}

export interface SignupResponse {
    userId: number;
    message: string;
}

export interface ApiError {
    message: string;
}